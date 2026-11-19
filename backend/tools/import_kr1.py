#!/usr/bin/env python3
"""Явный однократный перенос из КР1 в пустую БД КР3; PostgreSQL-клиент psql обязателен."""
import base64
import json
import os
import subprocess
import sys


# password_hash намеренно не выбирается даже из исходной БД.
SOURCE_COLUMNS = {
    "roles": "id, code",
    "users": "id, full_name, role_id, created_at",
    "user_credentials": "user_id, login",
    "brands": "id, name",
    "car_models": "id, brand_id, name",
    "body_types": "id, name",
    "transmissions": "id, name",
    "fuel_types": "id, name",
    "cars": "id, seller_id, model_id, year, mileage, price, status, created_at",
    "car_specifications": "car_id, vin, body_type_id, transmission_id, fuel_type_id, engine_volume, description",
    "purchase_requests": "id, user_id, car_id, message, status, created_at, updated_at",
}


def pg_environment(prefix):
    result = os.environ.copy()
    # Не наследуем параметры подключения к случайной рабочей БД.
    for name in list(result):
        if name.startswith("PG"):
            del result[name]
    for name in ("PGHOST", "PGPORT", "PGDATABASE", "PGUSER", "PGPASSWORD", "PGSSLMODE"):
        value = os.environ.get(prefix + "_" + name)
        if value is not None:
            result[name] = value
    for name in ("PGHOST", "PGPORT", "PGDATABASE", "PGUSER"):
        if not result.get(name):
            raise ValueError("Задайте " + prefix + "_" + name)
    result["PGCONNECT_TIMEOUT"] = "10"
    return result


def psql(environment, sql):
    # Пароли только в окружении дочернего процесса, SQL только через stdin.
    return subprocess.run(
        ["psql", "-X", "-q", "-A", "-t", "--no-password", "-v", "ON_ERROR_STOP=1"],
        input=sql, text=True, capture_output=True, env=environment, check=True,
    ).stdout


def read_source(environment):
    parts = []
    for table, columns in SOURCE_COLUMNS.items():
        parts.append(f"'{table}', (SELECT coalesce(json_agg(r), '[]'::json) "
                     f"FROM (SELECT {columns} FROM {table} ORDER BY 1) r)")
    sql = "BEGIN ISOLATION LEVEL REPEATABLE READ READ ONLY;\nSELECT json_build_object("
    sql += ",\n".join(parts) + ");\nCOMMIT;\n"
    data = json.loads(psql(environment, sql))
    data["timezone"] = os.environ.get("KR1_TIMEZONE", "Europe/Moscow")
    return data


def transfer_sql(data):
    # Только base64-алфавит попадает в SQL-литерал: содержимое записей не становится SQL.
    encoded = base64.b64encode(json.dumps(data, ensure_ascii=False).encode()).decode("ascii")
    statements = ["BEGIN;", "SET LOCAL TIME ZONE 'UTC';",
                  "LOCK TABLE roles, users, user_credentials, brands, car_models, body_types, "
                  "transmissions, fuel_types, cars, car_specifications, purchase_requests, car_tags, "
                  "audit_events IN SHARE ROW EXCLUSIVE MODE;",
                  "CREATE TEMP TABLE kr1_payload(data JSONB) ON COMMIT DROP;",
                  f"INSERT INTO kr1_payload VALUES (convert_from(decode('{encoded}', 'base64'), 'UTF8')::jsonb);"]
    statements.append("""
DO $$
DECLARE table_name TEXT;
DECLARE occupied BOOLEAN;
BEGIN
    IF to_regclass('flyway_schema_history') IS NULL THEN
        RAISE EXCEPTION 'Run Flyway migrations before importing';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM flyway_schema_history WHERE version = '3' AND success) THEN
        RAISE EXCEPTION 'Required migration V3 has not been applied';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_timezone_names WHERE name = (SELECT data->>'timezone' FROM kr1_payload)) THEN
        RAISE EXCEPTION 'Unknown source timezone';
    END IF;
    FOREACH table_name IN ARRAY ARRAY['users','user_credentials','brands','car_models','body_types',
        'transmissions','fuel_types','cars','car_specifications','purchase_requests','car_tags','audit_events'] LOOP
        EXECUTE format('SELECT EXISTS (SELECT 1 FROM %I)', table_name) INTO occupied;
        IF occupied THEN RAISE EXCEPTION 'Target must be empty: %', table_name; END IF;
    END LOOP;
    IF EXISTS (SELECT 1 FROM jsonb_to_recordset((SELECT data->'roles' FROM kr1_payload)) r(code TEXT)
               WHERE code NOT IN ('USER', 'ADMIN')) THEN
        RAISE EXCEPTION 'Unsupported legacy role';
    END IF;
END $$;
INSERT INTO users(id, full_name, role_id, enabled, must_change_password, created_at, updated_at)
SELECT u.id, u.full_name, target_role.id, FALSE, TRUE,
       u.created_at AT TIME ZONE (SELECT data->>'timezone' FROM kr1_payload),
       u.created_at AT TIME ZONE (SELECT data->>'timezone' FROM kr1_payload)
FROM jsonb_to_recordset((SELECT data->'users' FROM kr1_payload))
     u(id BIGINT, full_name TEXT, role_id BIGINT, created_at TIMESTAMP)
JOIN jsonb_to_recordset((SELECT data->'roles' FROM kr1_payload)) r(id BIGINT, code TEXT) ON r.id = u.role_id
JOIN roles target_role ON target_role.code = CASE r.code WHEN 'USER' THEN 'VIEWER' ELSE 'ADMIN' END;
INSERT INTO user_credentials(user_id, login, password_hash)
SELECT user_id, lower(btrim(login)), NULL
FROM jsonb_to_recordset((SELECT data->'user_credentials' FROM kr1_payload)) r(user_id BIGINT, login TEXT);
""")
    for table in ("brands", "body_types", "transmissions", "fuel_types"):
        statements.append(f"INSERT INTO {table}(id, name) SELECT id, name FROM "
                          f"jsonb_to_recordset((SELECT data->'{table}' FROM kr1_payload)) r(id BIGINT, name TEXT);")
    statements.append("""
INSERT INTO car_models(id, brand_id, name)
SELECT id, brand_id, name FROM jsonb_to_recordset((SELECT data->'car_models' FROM kr1_payload))
    r(id BIGINT, brand_id BIGINT, name TEXT);
INSERT INTO cars(id, external_code, seller_id, model_id, year, mileage, price, status, archived, created_at, updated_at)
SELECT id, 'KR1-' || id, seller_id, model_id, year, mileage, price,
       CASE status WHEN 'ARCHIVED' THEN 'AVAILABLE' ELSE status END, status = 'ARCHIVED',
       created_at AT TIME ZONE (SELECT data->>'timezone' FROM kr1_payload),
       created_at AT TIME ZONE (SELECT data->>'timezone' FROM kr1_payload)
FROM jsonb_to_recordset((SELECT data->'cars' FROM kr1_payload))
    r(id BIGINT, seller_id BIGINT, model_id BIGINT, year INTEGER, mileage INTEGER,
      price NUMERIC, status TEXT, created_at TIMESTAMP);
INSERT INTO car_specifications(car_id, vin, body_type_id, transmission_id, fuel_type_id, engine_volume, description)
SELECT car_id, nullif(upper(btrim(vin)), ''), body_type_id, transmission_id, fuel_type_id, engine_volume, description
FROM jsonb_to_recordset((SELECT data->'car_specifications' FROM kr1_payload))
    r(car_id BIGINT, vin TEXT, body_type_id BIGINT, transmission_id BIGINT, fuel_type_id BIGINT,
      engine_volume NUMERIC, description TEXT);
INSERT INTO purchase_requests(id, user_id, car_id, message, status, created_at, updated_at)
SELECT id, user_id, car_id, message, status,
       created_at AT TIME ZONE (SELECT data->>'timezone' FROM kr1_payload),
       updated_at AT TIME ZONE (SELECT data->>'timezone' FROM kr1_payload)
FROM jsonb_to_recordset((SELECT data->'purchase_requests' FROM kr1_payload))
    r(id BIGINT, user_id BIGINT, car_id BIGINT, message TEXT, status TEXT, created_at TIMESTAMP, updated_at TIMESTAMP);
INSERT INTO audit_events(action, entity_type, entity_id, details)
SELECT 'KR1_IMPORT', 'CAR', c.public_id::TEXT, jsonb_build_object('legacyId', r.id, 'legacyStatus', r.status)
FROM jsonb_to_recordset((SELECT data->'cars' FROM kr1_payload)) r(id BIGINT, status TEXT)
JOIN cars c ON c.id = r.id;
""")
    # Проверяем, что JOIN при переносе пользователей не потерял строки.
    for table in SOURCE_COLUMNS:
        if table == "roles":
            continue
        statements.append(f"DO $$ BEGIN IF (SELECT count(*) FROM {table}) <> "
                          f"(SELECT jsonb_array_length(data->'{table}') FROM kr1_payload) THEN "
                          f"RAISE EXCEPTION 'Row count mismatch: {table}'; END IF; END $$;")
    # setval вызывается только после проверок. Следующее штатное создание не конфликтует со старыми id.
    for table in ("users", "brands", "car_models", "body_types", "transmissions", "fuel_types", "cars", "purchase_requests"):
        statements.append(f"SELECT setval(pg_get_serial_sequence('{table}', 'id'), "
                          f"coalesce(max(id), 1), max(id) IS NOT NULL) FROM {table};")
    statements.append("COMMIT;")
    return "\n".join(statements)


def main():
    if sys.argv[1:] != ["--apply"]:
        print("Перенос запускается явно: python3 tools/import_kr1.py --apply", file=sys.stderr)
        return 2
    try:
        source = pg_environment("KR1")
        target = pg_environment("KR3")
        identity = ("PGHOST", "PGPORT", "PGDATABASE")
        if all(source[key] == target[key] for key in identity):
            raise ValueError("Базы источника и назначения должны различаться")
        data = read_source(source)
        psql(target, transfer_sql(data))
        print(f"Перенесено: пользователей {len(data['users'])}, автомобилей {len(data['cars'])}, "
              f"заявок {len(data['purchase_requests'])}. Пароли не копировались; пользователи заблокированы.")
        return 0
    except (ValueError, OSError, subprocess.CalledProcessError) as error:
        if isinstance(error, subprocess.CalledProcessError):
            print("Ошибка PostgreSQL: перенос не завершён. Проверьте схему, пустоту назначения и исходные данные.", file=sys.stderr)
        else:
            print(str(error), file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
