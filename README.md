# Маркетплейс автомобилей с пробегом

Консольное приложение на Java для учебной работы (КР1).

## Технологии

- Java 17
- Maven
- PostgreSQL
- JDBC
- Apache POI
- JUnit

## Сборка

```bash
mvn clean compile
```

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
- [ ] Этап 3 — модели и enum
- [ ] Этап 4 — JDBC и Repository
- [ ] Этап 5 — CRUD
- [ ] Этап 6 — бизнес-логика
- [ ] Этап 7 — поиск, фильтрация, сортировка
- [ ] Этап 8 — консольное меню
- [ ] Этап 9 — обработка ошибок ввода
- [ ] Этап 10 — статистика
- [ ] Этап 11 — экспорт в Excel
- [ ] Этап 12 — просмотр таблиц БД
- [ ] Этап 13 — финальная проверка
