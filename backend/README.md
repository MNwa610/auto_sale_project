# Backend автосалона — КР3

Самостоятельный Maven-проект. JavaFX-клиент КР2 находится в соседнем `../src/`
и продолжает собираться через `../pom.xml`. Backend не зависит от JavaFX.

## Состояние: этап 3

Реализованы запуск Spring Boot, базовый REST endpoint, валидация запросов,
общая обработка ошибок, OpenAPI/Swagger UI и Actuator health.

Подключены PostgreSQL, Flyway и JPA. При запуске Flyway применяет миграции,
затем Hibernate проверяет схему (`ddl-auto=validate`), не создавая таблиц.
Health проверяет в том числе соединение с БД. Backend не выполняет `../database.sql`.
Добавлены Spring Security, роли, BCrypt, access JWT, refresh-сессии,
смена пароля и ограничение попыток входа. Служебные endpoint ниже доступны без
входа. Frontend и Docker Compose пока не добавлены.
Контракт авторизации и первый вход: [`docs/AUTH.md`](docs/AUTH.md).

## Требования и запуск

- JDK 21; целевая версия компиляции — Java 21.
- Maven 3.9+.
- PostgreSQL 16 (для тестов — Docker с доступным daemon).
- Spring Boot 3.5.16, springdoc-openapi 2.10.0.

Версии закреплены в `pom.xml`. Совместимость веток указана в
[документации springdoc](https://springdoc.org/v2/), документация Spring Boot —
[ветка 3.5](https://docs.spring.io/spring-boot/3.5/).

Из каталога `auto_sale/backend`:

```bash
# Сначала подготовить отдельную БД и экспортировать настройки, см. ниже.
mvn clean verify
mvn spring-boot:run
```

Либо собрать и запустить исполняемый JAR:

```bash
mvn clean package
java -jar target/carmarket-backend-0.1.0-SNAPSHOT.jar
```

По умолчанию порт — `8080`. При уже экспортированных настройках БД можно выбрать другой:

```bash
SERVER_PORT=8081 java -jar target/carmarket-backend-0.1.0-SNAPSHOT.jar
```

Из каталога `auto_sale` можно использовать `mvn -f backend/pom.xml verify`.
Команды `mvn ...` без `-f backend/pom.xml` относятся к JavaFX-проекту КР2.

## Проверка API

- `GET http://localhost:8080/api/v1` — имя приложения и версия контракта.
- `GET http://localhost:8080/actuator/health` — состояние приложения.
- `http://localhost:8080/swagger-ui.html` — вход в Swagger UI (перенаправление на UI).
- `GET http://localhost:8080/v3/api-docs` — спецификация OpenAPI в JSON.

```bash
curl -i http://localhost:8080/api/v1
curl -i http://localhost:8080/actuator/health
curl -i http://localhost:8080/v3/api-docs
```

Ответ `/api/v1`:

```json
{"name":"carmarket-backend","apiVersion":"v1"}
```

Ошибки REST имеют тип `application/problem+json` (RFC 9457): `type`, `title`,
`status`, `detail`, `instance`, `timestamp`. Ошибки полей дополнительно содержат
`errors`: имя поля и список сообщений. Отклонённые значения и стек исключения
в ответ не включаются. Параметры query string не включаются в `instance`.
Неизвестные поля JSON отклоняются с HTTP 400.

Из Actuator наружу открыт только `health`, подробности компонентов скрыты.
Секреты в конфигурации отсутствуют; пароль БД задаётся через `DB_PASSWORD`,
ключ подписи JWT — через обязательный `JWT_SECRET_BASE64`.

## Подготовка PostgreSQL

Для КР3 используется отдельная БД `auto_sale_kr3`. JavaFX продолжает работать
со своей БД КР1/КР2. Не направляйте backend на заполненную БД КР2.

Администратор PostgreSQL создаёт отдельного владельца и базу через `psql`:

```sql
CREATE ROLE carmarket LOGIN;
\password carmarket
CREATE DATABASE auto_sale_kr3 OWNER carmarket;
```

`\password` интерактивно запрашивает локальный пароль. Реальные значения не
добавляются в репозиторий. Скопируйте `.env.example` в `.env`, замените пароль
и ключ JWT (сгенерировать `openssl rand -base64 32`), затем экспортируйте настройки:

```bash
set -a
. ./.env
set +a
mvn spring-boot:run
```

Spring Boot сам `.env` не загружает. Переменные: `DB_URL`, `DB_USER`,
обязательные `DB_PASSWORD` и `JWT_SECRET_BASE64`, необязательный `SERVER_PORT`.
`.env` игнорируется Git.
При отсутствии пароля/БД или несовпадении схемы приложение не должно успешно запускаться.

В миграциях есть только роли; автомобилей, пользователей и универсального
административного пароля нет. Для первого администратора предусмотрен явный
bootstrap из окружения с обязательной сменой временного пароля.
Аудит обычных действий, заметки и CSV API ещё не реализованы.

## Перенос данных КР1

Отдельная инструкция и правила: [`docs/DATABASE.md`](docs/DATABASE.md).
Инструмент `tools/import_kr1.py --apply` запускается вручную, требует Python 3
и `psql`. Источник читается в одной read-only транзакции; назначение должно
быть пустым после применения миграций. Автоматического переноса при запуске нет.

## Слои

- `controller` — REST endpoint; корневой путь API — `/api/v1`.
- `dto` — запросы и ответы API.
- `config` — конфигурация приложения и OpenAPI.
- `error` — общая обработка ошибок HTTP.
- `service` — бизнес-правила и транзакции.
- `repository` — доступ к PostgreSQL.
- `domain` — сущности и перечисления автосалона.
- `security` — роли, пароли, JWT и refresh-сессии.
- `filter` — поиск и фильтрация.
- `src/main/resources/db/migration` — применяемые миграции Flyway.

Пакеты без реализации обозначены через `package-info.java`; классы предметной
области добавляются на своих этапах, без заглушек с фиктивными данными.

## Проверки

`mvn verify` запускает одноразовую PostgreSQL 16 через Testcontainers,
загружает Spring-контекст и через MockMvc проверяет API, health,
OpenAPI, ресурсы Swagger UI, ошибки 400/404/405/500, валидацию и отсутствие
внутренних данных в ответах. Также проверяются Flyway, повторное применение,
JPA-связи, теги, ограничения БД и конфликт конкурентных обновлений.
Нужен работающий Docker; тесты не используют локальную рабочую БД и не
пропускаются молча при отсутствии Docker. Графическая среда не нужна.

Также проверяются авторизация, роли, политика паролей, истечение JWT,
refresh-ротация, отзыв, смена пароля, bootstrap и конкурентные запросы.
На следующих этапах добавляются тесты вычисляемого показателя и фильтров.
