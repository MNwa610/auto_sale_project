-- База данных для маркетплейса автомобилей с пробегом
-- PostgreSQL

DROP DATABASE IF EXISTS used_cars_marketplace;
CREATE DATABASE used_cars_marketplace;

\c used_cars_marketplace;

-- =====================
-- Таблица пользователей
-- =====================

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    full_name     VARCHAR(100) NOT NULL,
    login         VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT users_role_check
        CHECK (role IN ('USER', 'ADMIN'))
);

-- =====================
-- Таблица автомобилей
-- =====================

CREATE TABLE cars (
    id            BIGSERIAL PRIMARY KEY,
    seller_id     BIGINT         NOT NULL,
    brand         VARCHAR(50)    NOT NULL,
    model         VARCHAR(100)   NOT NULL,
    year          INTEGER        NOT NULL,
    mileage       INTEGER        NOT NULL,
    price         NUMERIC(12, 2) NOT NULL,
    vin           VARCHAR(17)    UNIQUE,
    body_type     VARCHAR(30)    NOT NULL,
    transmission  VARCHAR(30)    NOT NULL,
    fuel_type     VARCHAR(30)    NOT NULL,
    engine_volume NUMERIC(3, 1),
    description   TEXT,
    status        VARCHAR(20)    NOT NULL DEFAULT 'AVAILABLE',
    created_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT cars_seller_fk
        FOREIGN KEY (seller_id)
        REFERENCES users (id)
        ON DELETE RESTRICT,

    CONSTRAINT cars_year_check
        CHECK (year BETWEEN 1900 AND 2100),

    CONSTRAINT cars_mileage_check
        CHECK (mileage >= 0),

    CONSTRAINT cars_price_check
        CHECK (price > 0),

    CONSTRAINT cars_engine_volume_check
        CHECK (engine_volume IS NULL OR engine_volume > 0),

    CONSTRAINT cars_status_check
        CHECK (status IN ('AVAILABLE', 'RESERVED', 'SOLD', 'ARCHIVED'))
);

-- =====================
-- Таблица заявок на покупку
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

-- Автоматическое обновление updated_at при изменении заявки
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

CREATE INDEX idx_cars_brand ON cars (brand);
CREATE INDEX idx_cars_model ON cars (model);
CREATE INDEX idx_cars_price ON cars (price);
CREATE INDEX idx_cars_mileage ON cars (mileage);
CREATE INDEX idx_cars_seller ON cars (seller_id);
CREATE INDEX idx_cars_status ON cars (status);

CREATE INDEX idx_purchase_requests_user ON purchase_requests (user_id);
CREATE INDEX idx_purchase_requests_car ON purchase_requests (car_id);
CREATE INDEX idx_purchase_requests_status ON purchase_requests (status);

-- =====================
-- Тестовые данные
-- password_hash: для учебного проекта хранится как есть (позже можно хешировать в Java)
-- =====================

INSERT INTO users (full_name, login, password_hash, role) VALUES
('Администратор системы', 'admin',  'admin123', 'ADMIN'),
('Михаил Цепляев',        'misha',  '12345',    'USER'),
('Максим Коротаев',       'max',    '12345',    'USER'),
('Санджи Музраев',        'sano',   '12345',    'USER'),
('Андрей Деменьтьев',     'andrey', '12345',    'USER');

INSERT INTO cars (
    seller_id, brand, model, year, mileage, price, vin,
    body_type, transmission, fuel_type, engine_volume, description, status
) VALUES
(2, 'BMW',           'X5',     2021, 45000,  52000.00, 'WBA12345678901234', 'SUV',       'AUTOMATIC', 'PETROL', 3.0, 'Автомобиль в отличном состоянии',                    'AVAILABLE'),
(3, 'Toyota',        'Camry',  2020, 62000,  28500.00, 'JT123456789012345', 'SEDAN',     'AUTOMATIC', 'PETROL', 2.5, 'Надежный автомобиль',                              'AVAILABLE'),
(4, 'Mercedes-Benz', 'C200',   2019, 78000,  31000.00, 'WDD12345678901234', 'SEDAN',     'AUTOMATIC', 'PETROL', 2.0, 'Автомобиль после технического обслуживания',       'AVAILABLE'),
(5, 'Audi',          'A6',     2022, 30000,  47000.00, 'WAU12345678901234', 'SEDAN',     'AUTOMATIC', 'DIESEL', 2.0, 'Практически новый автомобиль',                     'AVAILABLE'),
(2, 'Volkswagen',    'Tiguan', 2021, 55000,  32000.00, 'WVG12345678901234', 'SUV',       'AUTOMATIC', 'PETROL', 2.0, 'Хорошее состояние',                                'AVAILABLE'),
(3, 'Kia',           'Sportage', 2020, 68000, 24000.00, 'KNA12345678901234', 'SUV',     'AUTOMATIC', 'PETROL', 2.0, 'Обслуженный автомобиль',                           'ARCHIVED'),
(4, 'Hyundai',       'Tucson', 2022, 35000,  29000.00, 'TMA12345678901234', 'SUV',       'AUTOMATIC', 'PETROL', 1.6, 'Один владелец',                                    'AVAILABLE'),
(5, 'Skoda',         'Octavia', 2019, 92000,  19000.00, 'TMB12345678901234', 'SEDAN',    'MANUAL',    'PETROL', 1.4, 'Экономичный автомобиль',                           'AVAILABLE'),
(2, 'Volvo',         'XC60',   2021, 41000,  43000.00, 'YV123456789012345', 'SUV',       'AUTOMATIC', 'DIESEL', 2.0, 'Без серьезных повреждений',                        'RESERVED'),
(3, 'Ford',          'Focus',  2018, 105000, 15000.00, 'WF123456789012345', 'HATCHBACK', 'MANUAL',    'PETROL', 1.6, 'Автомобиль для города',                            'SOLD');

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
