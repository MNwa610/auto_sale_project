-- Новая отдельная БД КР3. Схема и связи продолжают КР1; исходная БД не изменяется.
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE CHECK (code IN ('VIEWER', 'MANAGER', 'ADMIN')),
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL CHECK (btrim(full_name) <> ''),
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    must_change_password BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_credentials (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    login VARCHAR(50) NOT NULL UNIQUE CHECK (login = lower(btrim(login)) AND login <> ''),
    -- NULL означает, что пароль ещё не задан. Открытые пароли в КР3 не допускаются.
    password_hash VARCHAR(60),
    CONSTRAINT credentials_bcrypt CHECK (password_hash IS NULL OR
        password_hash ~ '^\$2[aby]\$(0[4-9]|[12][0-9]|3[01])\$[./A-Za-z0-9]{53}$')
);

CREATE TABLE brands (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE CHECK (btrim(name) <> ''),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX brands_name_ci ON brands(lower(btrim(name)));

CREATE TABLE car_models (
    id BIGSERIAL PRIMARY KEY,
    brand_id BIGINT NOT NULL REFERENCES brands(id) ON DELETE RESTRICT,
    name VARCHAR(100) NOT NULL CHECK (btrim(name) <> ''),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (brand_id, name)
);
CREATE UNIQUE INDEX car_models_name_ci ON car_models(brand_id, lower(btrim(name)));

CREATE TABLE body_types (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE CHECK (btrim(name) <> ''),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX body_types_name_ci ON body_types(lower(btrim(name)));

CREATE TABLE transmissions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE CHECK (btrim(name) <> ''),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX transmissions_name_ci ON transmissions(lower(btrim(name)));

CREATE TABLE fuel_types (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE CHECK (btrim(name) <> ''),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX fuel_types_name_ci ON fuel_types(lower(btrim(name)));

CREATE TABLE cars (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    external_code VARCHAR(64) NOT NULL UNIQUE CHECK (btrim(external_code) <> ''),
    seller_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    model_id BIGINT NOT NULL REFERENCES car_models(id) ON DELETE RESTRICT,
    year INTEGER NOT NULL CHECK (year BETWEEN 1900 AND 2100),
    mileage INTEGER NOT NULL CHECK (mileage >= 0),
    price NUMERIC(12,2) NOT NULL CHECK (price > 0),
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' CHECK (status IN ('AVAILABLE', 'RESERVED', 'SOLD')),
    certified BOOLEAN NOT NULL DEFAULT FALSE,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE car_specifications (
    car_id BIGINT PRIMARY KEY REFERENCES cars(id) ON DELETE CASCADE,
    vin VARCHAR(17) UNIQUE CHECK (vin IS NULL OR (length(vin) = 17 AND vin = upper(btrim(vin)))),
    body_type_id BIGINT NOT NULL REFERENCES body_types(id) ON DELETE RESTRICT,
    transmission_id BIGINT NOT NULL REFERENCES transmissions(id) ON DELETE RESTRICT,
    fuel_type_id BIGINT NOT NULL REFERENCES fuel_types(id) ON DELETE RESTRICT,
    engine_volume NUMERIC(3,1) CHECK (engine_volume IS NULL OR engine_volume > 0),
    description TEXT
);

CREATE TABLE car_tags (
    car_id BIGINT NOT NULL REFERENCES cars(id) ON DELETE CASCADE,
    tag VARCHAR(50) NOT NULL CHECK (tag = lower(btrim(tag)) AND tag <> ''),
    PRIMARY KEY (car_id, tag)
);

CREATE TABLE purchase_requests (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    car_id BIGINT NOT NULL REFERENCES cars(id) ON DELETE RESTRICT,
    message VARCHAR(1000),
    status VARCHAR(30) NOT NULL DEFAULT 'NEW'
        CHECK (status IN ('NEW', 'IN_PROGRESS', 'APPROVED', 'REJECTED', 'CANCELLED')),
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX users_role_idx ON users(role_id);
CREATE INDEX car_models_brand_idx ON car_models(brand_id);
CREATE INDEX cars_seller_idx ON cars(seller_id);
CREATE INDEX cars_model_idx ON cars(model_id);
CREATE INDEX cars_price_idx ON cars(price);
CREATE INDEX cars_mileage_idx ON cars(mileage);
CREATE INDEX cars_status_archive_idx ON cars(archived, status);
CREATE INDEX cars_created_idx ON cars(created_at, id);
CREATE INDEX car_tags_tag_idx ON car_tags(tag, car_id);
CREATE INDEX purchase_requests_user_idx ON purchase_requests(user_id);
CREATE INDEX purchase_requests_car_idx ON purchase_requests(car_id);
CREATE INDEX purchase_requests_status_idx ON purchase_requests(status);
