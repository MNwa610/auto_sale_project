# Backend автосалона — КР3

Самостоятельный Maven-проект. JavaFX-клиент КР2 находится в соседнем `../src/`
и продолжает собираться через `../pom.xml`. Backend не зависит от JavaFX.

## Состояние: этап 1

Реализованы запуск Spring Boot, базовый REST endpoint, валидация запросов,
общая обработка ошибок, OpenAPI/Swagger UI и Actuator health.

Подключение PostgreSQL, Flyway и JPA относится к этапу 2. Сейчас приложение
не подключается к БД и не выполняет `../database.sql`. Health со статусом `UP`
на этом этапе подтверждает работу приложения, но не доступность PostgreSQL.
Авторизация и ограничение доступа будут добавлены на этапе 3; текущие
служебные endpoint доступны без входа. Frontend и Docker пока не добавлены.

## Требования и запуск

- JDK 21; целевая версия компиляции — Java 21.
- Maven 3.9+.
- Spring Boot 3.5.16, springdoc-openapi 2.10.0.

Версии закреплены в `pom.xml`. Совместимость веток указана в
[документации springdoc](https://springdoc.org/v2/), документация Spring Boot —
[ветка 3.5](https://docs.spring.io/spring-boot/3.5/).

Из каталога `auto_sale/backend`:

```bash
mvn clean verify
mvn spring-boot:run
```

Либо собрать и запустить исполняемый JAR:

```bash
mvn clean package
java -jar target/carmarket-backend-0.1.0-SNAPSHOT.jar
```

По умолчанию порт — `8080`. Чтобы выбрать другой:

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
Секреты в конфигурации отсутствуют. Параметры БД и примеры окружения будут
добавлены вместе с соответствующей функциональностью.

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
- `src/main/resources/db/migration` — будущие миграции Flyway.

Пакеты без реализации обозначены через `package-info.java`; классы предметной
области добавляются на своих этапах, без заглушек с фиктивными данными.

## Проверки

`mvn verify` загружает Spring-контекст и через MockMvc проверяет API, health,
OpenAPI, ресурсы Swagger UI, ошибки 400/404/405/500, валидацию и отсутствие
внутренних данных в ответах. PostgreSQL и графическая среда для этих тестов не нужны.

На следующих этапах добавляются тесты вычисляемого показателя, фильтров,
безопасности и интеграционные проверки PostgreSQL/Flyway.
