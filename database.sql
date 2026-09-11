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

    CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'))
);

CREATE INDEX idx_users_login ON users (login);
CREATE INDEX idx_users_role ON users (role);

-- =====================
-- Таблица автомобилей
-- =====================

CREATE TABLE cars (
    id            BIGSERIAL PRIMARY KEY,
    seller_id     BIGINT       NOT NULL,
    brand         VARCHAR(50)  NOT NULL,
    model         VARCHAR(100) NOT NULL,
    year          INTEGER      NOT NULL,
    mileage       INTEGER      NOT NULL,
    price         NUMERIC(12, 2) NOT NULL,
    vin           VARCHAR(17)  UNIQUE,
    body_type     VARCHAR(30)  NOT NULL,
    transmission  VARCHAR(30)  NOT NULL,
    fuel_type     VARCHAR(30)  NOT NULL,
    engine_volume NUMERIC(3, 1),
    description   TEXT,
    status        VARCHAR(20)  NOT NULL DEFAULT 'AVAILABLE',
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_cars_seller
        FOREIGN KEY (seller_id) REFERENCES users (id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_cars_year CHECK (year >= 1900 AND year <= 2100),
    CONSTRAINT chk_cars_mileage CHECK (mileage >= 0),
    CONSTRAINT chk_cars_price CHECK (price > 0),
    CONSTRAINT chk_cars_status CHECK (status IN ('AVAILABLE', 'RESERVED', 'SOLD', 'ARCHIVED'))
);

CREATE INDEX idx_cars_seller_id ON cars (seller_id);
CREATE INDEX idx_cars_brand ON cars (brand);
CREATE INDEX idx_cars_status ON cars (status);
CREATE INDEX idx_cars_price ON cars (price);

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

    CONSTRAINT fk_purchase_requests_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_purchase_requests_car
        FOREIGN KEY (car_id) REFERENCES cars (id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_purchase_requests_status
        CHECK (status IN ('NEW', 'IN_PROGRESS', 'APPROVED', 'REJECTED', 'CANCELLED'))
);

CREATE INDEX idx_purchase_requests_user_id ON purchase_requests (user_id);
CREATE INDEX idx_purchase_requests_car_id ON purchase_requests (car_id);
CREATE INDEX idx_purchase_requests_status ON purchase_requests (status);

-- =====================
-- Тестовые данные
-- =====================

INSERT INTO users (full_name, login, password_hash, role) VALUES
('Иванов Иван Иванович',       'ivanov',    'hash_ivanov123',    'USER'),
('Петрова Анна Сергеевна',     'petrova',   'hash_petrova456',   'USER'),
('Сидоров Алексей Петрович',   'sidorov',   'hash_sidorov789',   'USER'),
('Кузнецова Мария Дмитриевна', 'kuznetsova','hash_kuznetsova01', 'USER'),
('Администратор Системы',      'admin',     'hash_admin999',     'ADMIN');

INSERT INTO cars (seller_id, brand, model, year, mileage, price, vin, body_type, transmission, fuel_type, engine_volume, description, status) VALUES
(1, 'Toyota',  'Camry',       2018, 85000,  1850000.00, 'JTDBT923885012345', 'sedan',       'automatic', 'petrol',  2.5, 'Один владелец, полная история обслуживания',           'AVAILABLE'),
(1, 'Hyundai', 'Solaris',     2019, 62000,  1150000.00, 'Z94CB41AAGR123456', 'sedan',       'manual',    'petrol',  1.6, 'Городская эксплуатация, без ДТП',                      'AVAILABLE'),
(2, 'Kia',     'Rio',         2020, 45000,  1350000.00, 'XWEHM512BL0001234', 'hatchback',   'automatic', 'petrol',  1.6, 'Комплектация Comfort, кондиционер',                    'RESERVED'),
(2, 'Volkswagen', 'Polo',     2017, 98000,  990000.00,  'WVWZZZ6RZHY123456', 'hatchback',   'manual',    'petrol',  1.6, 'Требуется замена резины',                              'AVAILABLE'),
(3, 'BMW',     'X5',          2016, 120000, 3200000.00, 'WBAFR9C50BC123456', 'crossover',   'automatic', 'diesel',  3.0, 'Полный привод, кожаный салон',                         'SOLD'),
(3, 'Lada',    'Vesta',       2021, 35000,  1050000.00, 'XTA219010M0123456', 'sedan',       'manual',    'petrol',  1.8, 'Новая резина, один хозяин',                            'AVAILABLE'),
(4, 'Skoda',   'Octavia',     2019, 71000,  1650000.00, 'TMBAB6NE9J0123456', 'liftback',    'robot',     'petrol',  1.4, 'Семейный автомобиль, бережная эксплуатация',           'AVAILABLE'),
(4, 'Renault', 'Duster',      2018, 89000,  1250000.00, 'X7LHSRHJN53123456', 'crossover',   'manual',    'petrol',  1.6, 'Подходит для загородных поездок',                       'ARCHIVED'),
(5, 'Mercedes-Benz', 'C-Class', 2015, 145000, 2100000.00, 'WDD2050421F123456', 'sedan',     'automatic', 'petrol',  2.0, 'Продажа от администратора для тестирования',             'AVAILABLE'),
(5, 'Ford',    'Focus',       2014, 156000, 750000.00,  '1FADP3F25EL123456', 'hatchback',   'manual',    'petrol',  1.6, 'Бюджетный вариант для первого автомобиля',             'AVAILABLE');

INSERT INTO purchase_requests (user_id, car_id, message, status) VALUES
(2, 1,  'Здравствуйте! Интересует Toyota Camry. Можно посмотреть в выходные?',           'NEW'),
(3, 1,  'Готов обсудить цену, если есть торг.',                                            'IN_PROGRESS'),
(4, 2,  'Хочу купить Hyundai Solaris для семьи.',                                          'APPROVED'),
(2, 3,  'Kia Rio подходит по бюджету, жду ответа.',                                        'IN_PROGRESS'),
(3, 6,  'Lada Vesta — актуально ли предложение?',                                          'NEW'),
(4, 7,  'Skoda Octavia нужна для ежедневных поездок на работу.',                           'REJECTED'),
(2, 9,  'Mercedes C-Class — можно ли организовать тест-драйв?',                            'NEW'),
(5, 10, 'Ford Focus интересует, но нужна скидка.',                                         'CANCELLED'),
(3, 4,  'Volkswagen Polo — когда удобно встретиться?',                                     'NEW'),
(4, 1,  'Camry всё ещё продаётся? Готов приехать сегодня.',                                'CANCELLED');
