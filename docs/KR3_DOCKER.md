# Запуск КР3 через Docker Compose

Нужен Docker с Compose v2 или новее и доступным Linux-движком. Выполняйте команды из корня репозитория.

## Первый запуск

В PowerShell создайте локальный файл окружения.

```powershell
Copy-Item .env.example .env
$key = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($key)
$rng.Dispose()
[Convert]::ToBase64String($key)
```

Заполните `.env` своим `DB_PASSWORD` и вставьте сгенерированный ключ в `JWT_SECRET_BASE64`.
Для создания первого администратора задайте `BOOTSTRAP_ADMIN_ENABLED=true`, `BOOTSTRAP_ADMIN_LOGIN` и `BOOTSTRAP_ADMIN_PASSWORD`.
Временный пароль должен содержать не менее 12 символов, заглавную и строчную буквы и цифру. Максимум — 72 байта UTF-8.
Значения с символом `$` заключите в одинарные кавычки в `.env`.

```powershell
docker compose config --quiet
docker compose up --build --wait
docker compose ps
```

Все три сервиса должны иметь статус `healthy`. Откройте веб-интерфейс и войдите под созданным администратором. После первого входа задайте новый пароль. [Автосалон](http://127.0.0.1:5173/)

После создания администратора установите `BOOTSTRAP_ADMIN_ENABLED=false`, очистите `BOOTSTRAP_ADMIN_LOGIN` и `BOOTSTRAP_ADMIN_PASSWORD` и повторно выполните `docker compose up -d --wait`. Сохраните тот же JWT-ключ и пароль PostgreSQL для следующих запусков.

По умолчанию API доступен на порту 8080, а интерфейс на 5173. Если порт занят, задайте другой `BACKEND_PORT` или `FRONTEND_PORT` в `.env`. React обращается к API через прокси внутри контейнера frontend.

## Проверка и остановка

```powershell
Invoke-RestMethod http://127.0.0.1:8080/actuator/health
Invoke-RestMethod http://127.0.0.1:5173/api/v1
docker compose logs backend
docker compose down
```

Первый запрос должен вернуть `UP`, второй — имя backend и версию API. Спецификация OpenAPI доступна на порту API. [Swagger UI](http://127.0.0.1:8080/swagger-ui.html)

PostgreSQL хранит данные в именованном томе `postgres_data`. После `docker compose down` можно снова выполнить `docker compose up -d --wait` и проверить, что созданные записи сохранились. Команда `docker compose down --volumes` удаляет том вместе с данными.

При первом запуске контейнер создаёт пустую БД КР3, а Flyway применяет миграции. Ранее настроенная локальная БД PostgreSQL живёт отдельно. Перенос данных из КР1 выполняется по инструкции. [Перенос БД](../backend/docs/DATABASE.md#явный-перенос-из-кр1)

## Автоматические тесты

Backend использует отдельный экземпляр PostgreSQL через Testcontainers. Для команд ниже нужны JDK 21, Maven, Node.js 22 и доступный движок Docker.

```powershell
mvn -f backend/pom.xml verify
npm.cmd --prefix frontend ci
npm.cmd --prefix frontend test
npm.cmd --prefix frontend run build
```
