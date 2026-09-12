# Маркетплейс автомобилей с пробегом

Консольное приложение на Java для учебной работы (КР1).

## Технологии

- Java 17
- Maven
- PostgreSQL
- JDBC
- Apache POI
- JUnit

## PostgreSQL

1. Создай базу `used_cars_marketplace` и выполни SQL-скрипт (таблицы, тестовые данные, индексы) в DBeaver.
2. Настрой подключение в `src/main/resources/database.properties`:

```properties
db.url=jdbc:postgresql://localhost:5432/used_cars_marketplace
db.user=postgres
db.password=...
```

При необходимости можно переопределить через переменные окружения: `DB_URL`, `DB_USER`, `DB_PASSWORD`.

**Тестовые логины** (из seed-данных): `admin` / `admin123` (ADMIN), `ivan`, `petr`, `alex`, `dmitry` (пароль `12345`).

## Сборка

```bash
mvn clean compile
```

## Тесты

```bash
mvn test
```

Интеграционные тесты репозитория (`RepositoryTest`, `CrudRepositoryTest`) требуют запущенный PostgreSQL с заполненной БД. Тесты сервисов работают без БД.

## Запуск (этап 1)

```bash
java -cp target/classes carmarket.Main
```

## Структура проекта

```
src/main/java/carmarket/
    Main.java
    model/
    enums/
    repository/
    service/
    ui/
    interfaces/
    exception/
    util/
```

## Статус разработки

- [x] Этап 1 — инициализация Maven-проекта
- [x] Этап 2 — схема базы данных PostgreSQL
- [x] Этап 3 — модели и enum
- [x] Этап 4 — JDBC и Repository
- [x] Этап 5 — CRUD
- [x] Этап 6 — бизнес-логика
- [ ] Этап 7 — поиск, фильтрация, сортировка
- [ ] Этап 8 — консольное меню
- [ ] Этап 9 — обработка ошибок ввода
- [ ] Этап 10 — статистика
- [ ] Этап 11 — экспорт в Excel
- [ ] Этап 12 — просмотр таблиц БД
- [ ] Этап 13 — финальная проверка
