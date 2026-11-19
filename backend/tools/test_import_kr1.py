#!/usr/bin/env python3
"""Проверка переноса и fail-fast валидации на одноразовых БД Docker, без рабочих данных."""
import argparse
import base64
import importlib.util
import json
import os
from pathlib import Path
import secrets
import subprocess
import time


BACKEND = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--java", default="java", help="Путь к Java 21+")
    args = parser.parse_args()
    jar = BACKEND / "target/carmarket-backend-0.1.0-SNAPSHOT.jar"
    if not jar.is_file():
        raise SystemExit("Сначала выполните mvn clean verify в backend")
    environment = os.environ.copy()
    environment["POSTGRES_PASSWORD"] = secrets.token_urlsafe(24)
    container = subprocess.check_output([
        "docker", "run", "--rm", "-d", "-e", "POSTGRES_PASSWORD",
        "-p", "127.0.0.1::5432", "postgres:16-alpine",
    ], text=True, env=environment).strip()

    def sql(database, statement):
        result = subprocess.run([
            "docker", "exec", "-i", container, "psql", "-X", "-qAt", "-U", "postgres",
            "-d", database, "-v", "ON_ERROR_STOP=1",
        ], input=statement, text=True, capture_output=True, check=True)
        return result.stdout.strip()

    def boot(database, expected_success=True):
        boot_env = environment.copy()
        boot_env.update(DB_URL=f"jdbc:postgresql://127.0.0.1:{port}/{database}",
                        DB_USER="postgres", DB_PASSWORD=environment["POSTGRES_PASSWORD"],
                        JWT_SECRET_BASE64=base64.b64encode(secrets.token_bytes(32)).decode(),
                        BOOTSTRAP_ADMIN_ENABLED="false")
        result = subprocess.run([
            args.java, "-jar", str(jar), "--spring.main.web-application-type=none", "--debug=false",
        ], env=boot_env, capture_output=True, text=True, timeout=60)
        output = result.stdout + result.stderr
        if expected_success:
            if result.returncode != 0:
                raise AssertionError("Backend не запустился: " + output[-4000:])
            assert "Started CarMarketBackendApplication" in output
        else:
            assert result.returncode != 0
        return output

    def importer(expected_code):
        result = subprocess.run(["python3", str(BACKEND / "tools/import_kr1.py"), "--apply"],
                                env=environment, text=True, capture_output=True, timeout=30)
        assert result.returncode == expected_code, result.stdout + result.stderr

    try:
        for attempt in range(100):
            check = subprocess.run(["docker", "exec", container, "pg_isready", "-U", "postgres"],
                                   capture_output=True)
            if check.returncode == 0:
                break
            time.sleep(0.2)
        else:
            raise AssertionError("Тестовая PostgreSQL не запустилась")
        port = json.loads(subprocess.check_output(["docker", "inspect", container], text=True))[0][
            "NetworkSettings"]["Ports"]["5432/tcp"][0]["HostPort"]
        sql("postgres", "CREATE DATABASE kr1_fixture; CREATE DATABASE kr3_fixture;")
        # DROP из старого скрипта выполняется ТОЛЬКО в новой одноразовой kr1_fixture этого контейнера.
        sql("kr1_fixture", (BACKEND.parent / "database.sql").read_text())
        boot("kr3_fixture")
        for prefix, database in (("KR1", "kr1_fixture"), ("KR3", "kr3_fixture")):
            for key, value in {"PGHOST": "127.0.0.1", "PGPORT": port, "PGUSER": "postgres",
                               "PGDATABASE": database, "PGPASSWORD": environment["POSTGRES_PASSWORD"]}.items():
                environment[prefix + "_" + key] = value

        # Коллизия после нормализации логина должна откатить даже уже вставленные профили.
        sql("kr1_fixture", "UPDATE user_credentials SET login='ADMIN' WHERE user_id=2;")
        importer(1)
        assert sql("kr3_fixture", "SELECT (SELECT count(*) FROM users)+(SELECT count(*) FROM cars)") == "0"
        sql("kr1_fixture", "UPDATE user_credentials SET login='fixture-seller' WHERE user_id=2;")

        spec = importlib.util.spec_from_file_location("import_kr1", BACKEND / "tools/import_kr1.py")
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        # Снимок всех исходных таблиц до/после импорта, без выбора password_hash.
        source_env = {key: value for key, value in environment.items() if not key.startswith("PG")}
        for key in ("PGHOST", "PGPORT", "PGUSER", "PGDATABASE", "PGPASSWORD"):
            source_env[key] = environment["KR1_" + key]
        before = module.read_source(source_env)
        importer(0)
        after = module.read_source(source_env)
        assert before == after
        assert sql("kr3_fixture", "SELECT count(*) FROM cars") == "10"
        assert sql("kr3_fixture", "SELECT count(*) FROM purchase_requests") == "10"
        assert sql("kr3_fixture", "SELECT count(*) FROM users WHERE NOT enabled AND must_change_password") == "5"
        assert sql("kr3_fixture", "SELECT count(*) FROM user_credentials WHERE password_hash IS NULL") == "5"
        assert sql("kr3_fixture", "SELECT archived AND status='AVAILABLE' FROM cars WHERE id=6") == "t"
        assert sql("kr3_fixture", "SELECT count(*) FROM cars WHERE public_id IS NOT NULL AND external_code='KR1-'||id") == "10"
        assert sql("kr3_fixture", "SELECT count(*) FROM audit_events WHERE action='KR1_IMPORT'") == "10"
        assert sql("kr3_fixture", "SELECT details->>'legacyStatus' FROM audit_events "
                   "WHERE details->>'legacyId'='6'") == "ARCHIVED"
        importer(1)
        assert sql("kr3_fixture", "SELECT count(*) FROM cars") == "10"
        assert sql("kr3_fixture", "SELECT nextval(pg_get_serial_sequence('cars','id'))") == "11"
        assert sql("kr3_fixture", "SELECT nextval(pg_get_serial_sequence('users','id'))") == "6"
        boot("kr3_fixture")
        print("OK: перенос 5 пользователей, 10 автомобилей и 10 заявок; источник неизменен.", flush=True)
        print("OK: откат при коллизии, запрет повторного переноса, UUID, архив, пароли и последовательности.", flush=True)

        sql("kr3_fixture", "ALTER TABLE cars RENAME COLUMN external_code TO damaged_external_code;")
        output = boot("kr3_fixture", expected_success=False)
        assert "missing column [external_code]" in output
        assert sql("kr3_fixture", "SELECT count(*) FROM information_schema.columns "
                   "WHERE table_name='cars' AND column_name='external_code'") == "0"
        print("OK: Hibernate отклоняет повреждённую схему и не исправляет её автоматически.", flush=True)
    finally:
        # Удаляется только контейнер с id, полученным этим запуском; чужие контейнеры не затрагиваются.
        subprocess.run(["docker", "stop", "--time", "5", container],
                       check=True, capture_output=True, timeout=30)


if __name__ == "__main__":
    main()
