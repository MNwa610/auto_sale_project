-- База данных для маркетплейса автомобилей с пробегом
-- PostgreSQL, третья нормальная форма (3НФ)
--
-- 1НФ: атомарные значения, нет повторяющихся групп.
-- 2НФ: нет частичных зависимостей (суррогатные PK).
-- 3НФ: нет транзитивных зависимостей.
--   Было: brand зависел от model, а не от cars.id (model → brand).
--   Стало: car_models.brand_id, cars.model_id.
-- Справочники и 1:1 таблицы убирают дубли и «толстые» сущности.

-- Скрипт выполняй в базе из database.properties (обычно auto_sale).
DROP TABLE IF EXISTS purchase_requests CASCADE;
DROP TABLE IF EXISTS car_specifications CASCADE;
DROP TABLE IF EXISTS cars CASCADE;
DROP TABLE IF EXISTS fuel_types CASCADE;
DROP TABLE IF EXISTS transmissions CASCADE;
DROP TABLE IF EXISTS body_types CASCADE;
DROP TABLE IF EXISTS car_models CASCADE;
DROP TABLE IF EXISTS brands CASCADE;
DROP TABLE IF EXISTS user_credentials CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS roles CASCADE;
DROP FUNCTION IF EXISTS set_purchase_requests_updated_at() CASCADE;

-- =====================
-- Справочник ролей
-- =====================

CREATE TABLE roles (
    id   BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL UNIQUE,

    CONSTRAINT roles_code_check
        CHECK (code IN ('USER', 'ADMIN'))
);

-- =====================
-- Пользователи (профиль)
-- =====================

CREATE TABLE users (
    id         BIGSERIAL PRIMARY KEY,
    full_name  VARCHAR(100) NOT NULL,
    role_id    BIGINT       NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT users_role_fk
        FOREIGN KEY (role_id)
        REFERENCES roles (id)
        ON DELETE RESTRICT
);

-- Учётные данные вынесены: login не зависит от ФИО
CREATE TABLE user_credentials (
    user_id       BIGINT       PRIMARY KEY,
    login         VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,

    CONSTRAINT user_credentials_user_fk
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE
);

-- =====================
-- Справочники автомобиля
-- =====================

CREATE TABLE brands (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- Модель определяет марку (3НФ)
CREATE TABLE car_models (
    id       BIGSERIAL PRIMARY KEY,
    brand_id BIGINT      NOT NULL,
    name     VARCHAR(100) NOT NULL,

    CONSTRAINT car_models_brand_fk
        FOREIGN KEY (brand_id)
        REFERENCES brands (id)
        ON DELETE RESTRICT,

    CONSTRAINT car_models_brand_name_uk
        UNIQUE (brand_id, name)
);

CREATE TABLE body_types (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE transmissions (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE fuel_types (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE
);

-- =====================
-- Объявление о продаже
-- =====================

CREATE TABLE cars (
    id         BIGSERIAL PRIMARY KEY,
    seller_id  BIGINT         NOT NULL,
    model_id   BIGINT         NOT NULL,
    year       INTEGER        NOT NULL,
    mileage    INTEGER        NOT NULL,
    price      NUMERIC(12, 2) NOT NULL,
    status     VARCHAR(20)    NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT cars_seller_fk
        FOREIGN KEY (seller_id)
        REFERENCES users (id)
        ON DELETE RESTRICT,

    CONSTRAINT cars_model_fk
        FOREIGN KEY (model_id)
        REFERENCES car_models (id)
        ON DELETE RESTRICT,

    CONSTRAINT cars_year_check
        CHECK (year BETWEEN 1900 AND 2100),

    CONSTRAINT cars_mileage_check
        CHECK (mileage >= 0),

    CONSTRAINT cars_price_check
        CHECK (price > 0),

    CONSTRAINT cars_status_check
        CHECK (status IN ('AVAILABLE', 'RESERVED', 'SOLD', 'ARCHIVED'))
);

-- Технические характеристики: 1:1 с объявлением
CREATE TABLE car_specifications (
    car_id         BIGINT PRIMARY KEY,
    vin            VARCHAR(17) UNIQUE,
    body_type_id   BIGINT NOT NULL,
    transmission_id BIGINT NOT NULL,
    fuel_type_id   BIGINT NOT NULL,
    engine_volume  NUMERIC(3, 1),
    description    TEXT,

    CONSTRAINT car_specifications_car_fk
        FOREIGN KEY (car_id)
        REFERENCES cars (id)
        ON DELETE CASCADE,

    CONSTRAINT car_specifications_body_type_fk
        FOREIGN KEY (body_type_id)
        REFERENCES body_types (id)
        ON DELETE RESTRICT,

    CONSTRAINT car_specifications_transmission_fk
        FOREIGN KEY (transmission_id)
        REFERENCES transmissions (id)
        ON DELETE RESTRICT,

    CONSTRAINT car_specifications_fuel_type_fk
        FOREIGN KEY (fuel_type_id)
        REFERENCES fuel_types (id)
        ON DELETE RESTRICT,

    CONSTRAINT car_specifications_engine_volume_check
        CHECK (engine_volume IS NULL OR engine_volume > 0)
);

-- =====================
-- Заявки на покупку
-- =====================

CREATE TABLE purchase_requests (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    car_id     BIGINT       NOT NULL,
    message    VARCHAR(1000),
    status     VARCHAR(30)  NOT NULL DEFAULT 'NEW',
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT purchase_requests_user_fk
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE RESTRICT,

    CONSTRAINT purchase_requests_car_fk
        FOREIGN KEY (car_id)
        REFERENCES cars (id)
        ON DELETE RESTRICT,

    CONSTRAINT purchase_requests_status_check
        CHECK (status IN ('NEW', 'IN_PROGRESS', 'APPROVED', 'REJECTED', 'CANCELLED'))
);

CREATE OR REPLACE FUNCTION set_purchase_requests_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_purchase_requests_updated_at
    BEFORE UPDATE ON purchase_requests
    FOR EACH ROW
    EXECUTE PROCEDURE set_purchase_requests_updated_at();

-- =====================
-- Индексы
-- =====================

CREATE INDEX idx_users_role ON users (role_id);
CREATE INDEX idx_car_models_brand ON car_models (brand_id);
CREATE INDEX idx_cars_seller ON cars (seller_id);
CREATE INDEX idx_cars_model ON cars (model_id);
CREATE INDEX idx_cars_price ON cars (price);
CREATE INDEX idx_cars_mileage ON cars (mileage);
CREATE INDEX idx_cars_status ON cars (status);

CREATE INDEX idx_purchase_requests_user ON purchase_requests (user_id);
CREATE INDEX idx_purchase_requests_car ON purchase_requests (car_id);
CREATE INDEX idx_purchase_requests_status ON purchase_requests (status);

-- =====================
-- Тестовые данные
-- =====================

INSERT INTO roles (code, name) VALUES
('USER',  'Пользователь'),
('ADMIN', 'Администратор');

INSERT INTO users (full_name, role_id) VALUES
('Администратор системы', (SELECT id FROM roles WHERE code = 'ADMIN')),
('Михаил Цепляев',        (SELECT id FROM roles WHERE code = 'USER')),
('Максим Коротаев',       (SELECT id FROM roles WHERE code = 'USER')),
('Санджи Музраев',        (SELECT id FROM roles WHERE code = 'USER')),
('Андрей Деменьтьев',     (SELECT id FROM roles WHERE code = 'USER'));

INSERT INTO user_credentials (user_id, login, password_hash) VALUES
(1, 'admin',  'admin123'),
(2, 'misha',  '12345'),
(3, 'max',    '12345'),
(4, 'sano',   '12345'),
(5, 'andrey', '12345');

INSERT INTO brands (name) VALUES
('BMW'), ('Toyota'), ('Mercedes-Benz'), ('Audi'), ('Volkswagen'),
('Kia'), ('Hyundai'), ('Skoda'), ('Volvo'), ('Ford');

INSERT INTO car_models (brand_id, name) VALUES
((SELECT id FROM brands WHERE name = 'BMW'),            'X5'),
((SELECT id FROM brands WHERE name = 'Toyota'),         'Camry'),
((SELECT id FROM brands WHERE name = 'Mercedes-Benz'),  'C200'),
((SELECT id FROM brands WHERE name = 'Audi'),           'A6'),
((SELECT id FROM brands WHERE name = 'Volkswagen'),     'Tiguan'),
((SELECT id FROM brands WHERE name = 'Kia'),            'Sportage'),
((SELECT id FROM brands WHERE name = 'Hyundai'),        'Tucson'),
((SELECT id FROM brands WHERE name = 'Skoda'),          'Octavia'),
((SELECT id FROM brands WHERE name = 'Volvo'),          'XC60'),
((SELECT id FROM brands WHERE name = 'Ford'),           'Focus');

INSERT INTO body_types (name) VALUES ('SUV'), ('SEDAN'), ('HATCHBACK');
INSERT INTO transmissions (name) VALUES ('AUTOMATIC'), ('MANUAL');
INSERT INTO fuel_types (name) VALUES ('PETROL'), ('DIESEL');

INSERT INTO cars (seller_id, model_id, year, mileage, price, status) VALUES
(2, (SELECT id FROM car_models WHERE name = 'X5'),       2021, 45000,  52000.00, 'AVAILABLE'),
(3, (SELECT id FROM car_models WHERE name = 'Camry'),    2020, 62000,  28500.00, 'AVAILABLE'),
(4, (SELECT id FROM car_models WHERE name = 'C200'),     2019, 78000,  31000.00, 'AVAILABLE'),
(5, (SELECT id FROM car_models WHERE name = 'A6'),       2022, 30000,  47000.00, 'AVAILABLE'),
(2, (SELECT id FROM car_models WHERE name = 'Tiguan'),   2021, 55000,  32000.00, 'AVAILABLE'),
(3, (SELECT id FROM car_models WHERE name = 'Sportage'), 2020, 68000,  24000.00, 'ARCHIVED'),
(4, (SELECT id FROM car_models WHERE name = 'Tucson'),   2022, 35000,  29000.00, 'AVAILABLE'),
(5, (SELECT id FROM car_models WHERE name = 'Octavia'),  2019, 92000,  19000.00, 'AVAILABLE'),
(2, (SELECT id FROM car_models WHERE name = 'XC60'),     2021, 41000,  43000.00, 'RESERVED'),
(3, (SELECT id FROM car_models WHERE name = 'Focus'),    2018, 105000, 15000.00, 'SOLD');

INSERT INTO car_specifications (
    car_id, vin, body_type_id, transmission_id, fuel_type_id, engine_volume, description
) VALUES
(1,  'WBA12345678901234', (SELECT id FROM body_types WHERE name = 'SUV'),
     (SELECT id FROM transmissions WHERE name = 'AUTOMATIC'),
     (SELECT id FROM fuel_types WHERE name = 'PETROL'), 3.0, 'Автомобиль в отличном состоянии'),
(2,  'JT123456789012345', (SELECT id FROM body_types WHERE name = 'SEDAN'),
     (SELECT id FROM transmissions WHERE name = 'AUTOMATIC'),
     (SELECT id FROM fuel_types WHERE name = 'PETROL'), 2.5, 'Надежный автомобиль'),
(3,  'WDD12345678901234', (SELECT id FROM body_types WHERE name = 'SEDAN'),
     (SELECT id FROM transmissions WHERE name = 'AUTOMATIC'),
     (SELECT id FROM fuel_types WHERE name = 'PETROL'), 2.0, 'Автомобиль после технического обслуживания'),
(4,  'WAU12345678901234', (SELECT id FROM body_types WHERE name = 'SEDAN'),
     (SELECT id FROM transmissions WHERE name = 'AUTOMATIC'),
     (SELECT id FROM fuel_types WHERE name = 'DIESEL'), 2.0, 'Практически новый автомобиль'),
(5,  'WVG12345678901234', (SELECT id FROM body_types WHERE name = 'SUV'),
     (SELECT id FROM transmissions WHERE name = 'AUTOMATIC'),
     (SELECT id FROM fuel_types WHERE name = 'PETROL'), 2.0, 'Хорошее состояние'),
(6,  'KNA12345678901234', (SELECT id FROM body_types WHERE name = 'SUV'),
     (SELECT id FROM transmissions WHERE name = 'AUTOMATIC'),
     (SELECT id FROM fuel_types WHERE name = 'PETROL'), 2.0, 'Обслуженный автомобиль'),
(7,  'TMA12345678901234', (SELECT id FROM body_types WHERE name = 'SUV'),
     (SELECT id FROM transmissions WHERE name = 'AUTOMATIC'),
     (SELECT id FROM fuel_types WHERE name = 'PETROL'), 1.6, 'Один владелец'),
(8,  'TMB12345678901234', (SELECT id FROM body_types WHERE name = 'SEDAN'),
     (SELECT id FROM transmissions WHERE name = 'MANUAL'),
     (SELECT id FROM fuel_types WHERE name = 'PETROL'), 1.4, 'Экономичный автомобиль'),
(9,  'YV123456789012345', (SELECT id FROM body_types WHERE name = 'SUV'),
     (SELECT id FROM transmissions WHERE name = 'AUTOMATIC'),
     (SELECT id FROM fuel_types WHERE name = 'DIESEL'), 2.0, 'Без серьезных повреждений'),
(10, 'WF123456789012345', (SELECT id FROM body_types WHERE name = 'HATCHBACK'),
     (SELECT id FROM transmissions WHERE name = 'MANUAL'),
     (SELECT id FROM fuel_types WHERE name = 'PETROL'), 1.6, 'Автомобиль для города');

INSERT INTO purchase_requests (user_id, car_id, message, status) VALUES
(3, 1,  'Интересует покупка BMW X5',                         'NEW'),
(4, 2,  'Хотел бы получить дополнительную информацию',       'IN_PROGRESS'),
(5, 3,  'Готов обсудить условия покупки',                    'APPROVED'),
(2, 4,  'Интересует данный автомобиль',                      'NEW'),
(3, 5,  'Можно ли посмотреть автомобиль?',                   'NEW'),
(4, 6,  'Интересует состояние автомобиля',                   'REJECTED'),
(5, 7,  'Хочу приобрести автомобиль',                        'IN_PROGRESS'),
(2, 8,  'Интересует Skoda Octavia',                          'CANCELLED'),
(3, 9,  'Хотел бы забронировать автомобиль',                 'APPROVED'),
(4, 10, 'Интересует автомобиль',                             'REJECTED');
