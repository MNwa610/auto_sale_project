# Маркетплейс автомобилей с пробегом

Учебное JavaFX-приложение (КР №2) на основе консольной КР №1. Работа ведётся в `main`. Сдача КР №1 зафиксирована тегом `kr1-final`.

## Статус КР №2

- [x] Этап 1 — Maven, Java 21, JavaFX, пустое окно. Эталонный `pom.xml` преподавателя (свои `groupId`/`artifactId`/`mainClass`, добавлен Apache POI). Консольный `ui/` перенесён в `legacy-console/` (`git mv`), Maven его не компилирует. Запуск: `carmarket.fx.CarMarketApplication` и `fxml/main-view.fxml` (заголовок и пустой `BorderPane`).
- [x] Этап 2 — подключение к PostgreSQL КР №1 по JDBC (`database.properties`, `DatabaseManager`, проверка связи при старте, `findAll` и JOIN для заявок).
- [ ] Этап 3 — главное окно и TableView заявок
- [ ] Этап 4 — добавление и редактирование заявки
- [ ] Этап 5 — удаление и смена статуса
- [ ] Этап 6 — валидация и обработка ошибок
- [ ] Этап 7 — вкладка «Автомобили»
- [ ] Этап 8 — вкладка «Пользователи»
- [ ] Этап 9 — поиск, фильтрация, сортировка
- [ ] Этап 10 — просмотр связанных данных
- [ ] Этап 11 — статистика
- [ ] Этап 12 — экспорт
- [ ] Этап 13 — CSS
- [ ] Этап 14 — тесты, README, подготовка к защите

Jackson в `pom.xml` оставлен из эталонного pom преподавателя, в КР №2 не используется.

## Технологии

- Java 21
- JavaFX 21.0.8
- Maven 3.9+
- PostgreSQL
- JDBC
- FXML, CSS
- Apache POI
- JUnit 5, AssertJ
- slf4j

## PostgreSQL

1. Создай базу `auto_sale`.
2. При необходимости выполни `database.sql` в DBeaver. **Внимание:** скрипт начинается с `DROP TABLE ... CASCADE` и удаляет существующие таблицы. Не запускай его на заполненной базе без необходимости.
3. Скопируй `src/main/resources/database.properties.example` в `src/main/resources/database.properties` и укажи пароль. Файл с паролем в git не попадает.

```properties
db.url=jdbc:postgresql://localhost:5432/auto_sale
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

## Запуск

```bash
mvn javafx:run
```

Если PostgreSQL не запущен или параметры в `database.properties` неверны, приложение открывает окно и показывает понятное сообщение об ошибке.

## Структура проекта

```
src/main/java/carmarket/
    Main.java
    fx/
    controller/
    model/
    enums/
    repository/
    service/
    interfaces/
    exception/
    util/
src/main/resources/
    fxml/
    css/
    database.properties.example
legacy-console/carmarket/ui/   — консоль КР №1, не входит в сборку
```

## Статус разработки КР №1

- [x] Этап 1 — инициализация Maven-проекта
- [x] Этап 2 — схема базы данных PostgreSQL
- [x] Этап 3 — модели и enum
- [x] Этап 4 — JDBC и Repository
- [x] Этап 5 — CRUD
- [x] Этап 6 — бизнес-логика
- [x] Этап 7 — поиск, фильтрация, сортировка
- [x] Этап 8 — консольное меню
- [x] Этап 9 — обработка ошибок ввода
- [x] Этап 10 — статистика
- [x] Этап 11 — экспорт в Excel
- [x] Этап 12 — просмотр таблиц БД
- [x] Этап 13 — финальная проверка

## Назначение проекта

Проект представляет собой консольный маркетплейс автомобилей с пробегом.

Основные сущности:
- `PurchaseRequest` - заявка пользователя на покупку автомобиля
- `User` - пользователь системы
- `Car` — автомобиль, размещённый на продаже
- `PurchaseRequest` — заявка пользователя на покупку автомобиля

Связи между сущностями:

```
User 1:N Car
User 1:N PurchaseRequest
Car  1:N PurchaseRequest
```

Обычный пользователь работает с автомобилями и своими заявками. Администратор имеет дополнительные возможности управления данными и просмотра статистики.

## Архитектура проекта

В проекте используется простая многослойная архитектура:

```
Console UI
    ↓
Service
    ↓
Repository
    ↓
JDBC
    ↓
PostgreSQL
```

### Console UI

Отвечает за:

- меню программы
- ввод данных пользователя
- вывод результатов
- обработку действий пользователя

### Service

Отвечает за:

- бизнес-логику
- проверки данных
- выполнение бизнес-правил
- вызов Repository

### Repository

Отвечает за:

- SQL-запросы
- работу с JDBC
- CRUD-операции
- получение данных из PostgreSQL

### DatabaseManager

Отвечает за создание подключения к PostgreSQL.

## Основные возможности

### Обычный пользователь

Обычный пользователь может:

- просматривать автомобили
- находить автомобиль по марке или модели
- просматривать автомобиль по ID
- подавать заявку на покупку
- просматривать свои заявки
- просматривать свои данные
- выходить из аккаунта

### Администратор

Администратор может:

- просматривать пользователей
- работать с автомобилями
- работать с заявками
- выполнять поиск
- выполнять фильтрацию
- выполнять сортировку
- просматривать статистику
- экспортировать данные в Excel
- просматривать данные таблиц базы данных
- выходить из аккаунта

## Бизнес-правила

В проекте реализованы следующие основные правила:

1. Нельзя создать заявку на несуществующий автомобиль
2. Нельзя создать заявку от несуществующего пользователя
3. Нельзя создать заявку на автомобиль со статусом `SOLD` или `ARCHIVED`
4. Пользователь не может иметь более одной активной заявки на один автомобиль
5. Цена автомобиля должна быть больше 0
6. Пробег автомобиля не может быть отрицательным
7. Для заявок разрешены только определённые переходы между статусами

Активные статусы заявки:

```
NEW
IN_PROGRESS
```

Разрешённые переходы:

```
NEW -> IN_PROGRESS
NEW -> CANCELLED

IN_PROGRESS -> APPROVED
IN_PROGRESS -> REJECTED
IN_PROGRESS -> CANCELLED
```

Для статусов `APPROVED`, `REJECTED` и `CANCELLED` дальнейшие переходы не разрешены.

## Поиск

В проекте реализован поиск.

### Автомобили

Поиск выполняется по:

- марке автомобиля
- модели автомобиля

### Заявки

Поиск выполняется по имени пользователя.

## Фильтрация

Реализована фильтрация:

- заявок по статусу
- автомобилей по диапазону цены

## Сортировка

Реализована сортировка:

- автомобилей по цене
- автомобилей по пробегу

Для работы с коллекциями используются стандартные средства Java: `List`, `Map`, `Set`, `Comparator` и `Stream API` там, где это упрощает код.

## Статистика

Программа предоставляет статистику по данным PostgreSQL.

Выводятся:

- количество пользователей
- количество автомобилей
- количество заявок
- количество заявок со статусом `NEW`
- количество заявок со статусом `IN_PROGRESS`
- количество заявок со статусом `APPROVED`
- количество заявок со статусом `REJECTED`
- количество заявок со статусом `CANCELLED`
- средняя цена автомобиля
- средний пробег автомобиля

## Экспорт в Excel

Для экспорта используется библиотека Apache POI.

Основной файл экспорта:

```
marketplace_export.xlsx
```

В Excel создаются листы:

```
Users
Cars
PurchaseRequests
```

Доступны следующие операции:

```
1. Экспорт пользователей
2. Экспорт автомобилей
3. Экспорт заявок
4. Экспорт всех данных
5. Возврат в предыдущее меню
```

Данные для Excel берутся из PostgreSQL через существующие Service и Repository.

## Просмотр таблиц базы данных

В административном меню предусмотрен пункт:

```
9. Таблицы БД
```

После выбора открывается меню:

```
1. Users
2. Cars
3. PurchaseRequests
4. Назад
```

Данные получают через существующие Service и Repository, а SQL находится в Repository.

## Обработка ошибок ввода

`InputHelper` обрабатывает некорректный ввод пользователя, в том числе:

- строку вместо `int`
- строку вместо `long`
- строку вместо `double`
- пустую строку
- отрицательный ID
- несуществующий ID
- некорректную цену
- некорректный пробег

При ошибке программа выводит сообщение и продолжает работу.

## Структура базы данных

Основные таблицы PostgreSQL:

### `users`

Хранит пользователей системы.

Основные данные:

- идентификатор
- ФИО
- логин
- пароль
- роль
- дата создания

Роли:

```
USER
ADMIN
```

### `cars`

Хранит автомобили, размещённые на продаже.

Основные данные:

- продавец
- марка
- модель
- год выпуска
- пробег
- цена
- VIN
- тип кузова
- коробка передач
- тип топлива
- объём двигателя
- описание
- статус
- дата создания

Статусы автомобилей:

```
AVAILABLE
RESERVED
SOLD
ARCHIVED
```

### `purchase_requests`

Хранит заявки пользователей на покупку автомобилей.

Основные данные:

- пользователь
- автомобиль
- сообщение
- статус
- дата создания
- дата обновления

Статусы заявок:

```
NEW
IN_PROGRESS
APPROVED
REJECTED
CANCELLED
```

## ER-диаграмма

```mermaid
erDiagram
    ROLES ||--o{ USERS : "has"
    USERS ||--|| USER_CREDENTIALS : "has credentials"

    BRANDS ||--o{ CAR_MODELS : "contains"
    CAR_MODELS ||--o{ CARS : "used in"

    USERS ||--o{ CARS : "sells"
    CARS ||--|| CAR_SPECIFICATIONS : "has specifications"

    BODY_TYPES ||--o{ CAR_SPECIFICATIONS : "defines"
    TRANSMISSIONS ||--o{ CAR_SPECIFICATIONS : "defines"
    FUEL_TYPES ||--o{ CAR_SPECIFICATIONS : "defines"

    USERS ||--o{ PURCHASE_REQUESTS : "creates"
    CARS ||--o{ PURCHASE_REQUESTS : "receives"

    ROLES {
        BIGSERIAL id PK
        VARCHAR_20 code UK
        VARCHAR_50 name UK
    }

    USERS {
        BIGSERIAL id PK
        VARCHAR_100 full_name
        BIGINT role_id FK
        TIMESTAMP created_at
    }

    USER_CREDENTIALS {
        BIGINT user_id PK, FK
        VARCHAR_50 login UK
        VARCHAR_255 password_hash
    }

    BRANDS {
        BIGSERIAL id PK
        VARCHAR_50 name UK
    }

    CAR_MODELS {
        BIGSERIAL id PK
        BIGINT brand_id FK
        VARCHAR_100 name
    }

    BODY_TYPES {
        BIGSERIAL id PK
        VARCHAR_30 name UK
    }

    TRANSMISSIONS {
        BIGSERIAL id PK
        VARCHAR_30 name UK
    }

    FUEL_TYPES {
        BIGSERIAL id PK
        VARCHAR_30 name UK
    }

    CARS {
        BIGSERIAL id PK
        BIGINT seller_id FK
        BIGINT model_id FK
        INTEGER year
        INTEGER mileage
        NUMERIC_12_2 price
        VARCHAR_20 status
        TIMESTAMP created_at
    }

    CAR_SPECIFICATIONS {
        BIGINT car_id PK, FK
        VARCHAR_17 vin UK
        BIGINT body_type_id FK
        BIGINT transmission_id FK
        BIGINT fuel_type_id FK
        NUMERIC_3_1 engine_volume
        TEXT description
    }

    PURCHASE_REQUESTS {
        BIGSERIAL id PK
        BIGINT user_id FK
        BIGINT car_id FK
        VARCHAR_1000 message
        VARCHAR_30 status
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }
```



Основные связи между таблицами:

```
ROLES 1:N USERS - одна роль может принадлежать многим пользователям.
USERS 1:1 USER_CREDENTIALS - учётные данные принадлежат одному пользователю; user_id одновременно PK и FK.
BRANDS 1:N CAR_MODELS - у одной марки может быть много моделей.
CAR_MODELS 1:N CARS - одна модель может использоваться в нескольких объявлениях.
USERS 1:N CARS - один пользователь может продавать несколько автомобилей.
CARS 1:1 CAR_SPECIFICATIONS - технические характеристики вынесены в отдельную таблицу; car_id одновременно PK и FK.
BODY_TYPES 1:N CAR_SPECIFICATIONS - один тип кузова может использоваться у многих автомобилей.
TRANSMISSIONS 1:N CAR_SPECIFICATIONS - одна коробка передач может использоваться у многих автомобилей.
FUEL_TYPES 1:N CAR_SPECIFICATIONS - один тип топлива может использоваться у многих автомобилей.
USERS 1:N PURCHASE_REQUESTS - один пользователь может создать несколько заявок.
CARS 1:N PURCHASE_REQUESTS - на один автомобиль может быть несколько заявок.
```
