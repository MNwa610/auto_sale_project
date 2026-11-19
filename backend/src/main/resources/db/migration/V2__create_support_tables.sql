-- Только структуры для следующих этапов, без реализации авторизации/заметок/CSV.
CREATE TABLE refresh_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    replaced_by UUID REFERENCES refresh_sessions(id) ON DELETE SET NULL,
    CHECK (expires_at > created_at)
);
CREATE INDEX refresh_sessions_user_idx ON refresh_sessions(user_id);
CREATE INDEX refresh_sessions_expiry_idx ON refresh_sessions(expires_at);

CREATE TABLE login_attempts (
    login VARCHAR(50) PRIMARY KEY,
    failed_attempts INTEGER NOT NULL DEFAULT 0 CHECK (failed_attempts >= 0),
    last_failed_at TIMESTAMPTZ NOT NULL,
    locked_until TIMESTAMPTZ
);

CREATE TABLE car_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    car_id BIGINT NOT NULL REFERENCES cars(id) ON DELETE CASCADE,
    author_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    content VARCHAR(4000) NOT NULL CHECK (btrim(content) <> ''),
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX car_notes_car_idx ON car_notes(car_id, pinned DESC, created_at DESC);
CREATE INDEX car_notes_author_idx ON car_notes(author_id);

CREATE TABLE audit_events (
    id BIGSERIAL PRIMARY KEY,
    actor_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    -- Без FK: история сохраняется после окончательного удаления объекта.
    entity_id VARCHAR(64),
    details JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX audit_events_created_idx ON audit_events(created_at, id);
CREATE INDEX audit_events_action_idx ON audit_events(action);
CREATE INDEX audit_events_actor_idx ON audit_events(actor_id);
CREATE INDEX audit_events_entity_idx ON audit_events(entity_type, entity_id);

CREATE TABLE csv_import_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    mode VARCHAR(30) NOT NULL CHECK (mode IN ('NEW_ONLY', 'UPDATE_BY_EXTERNAL_CODE')),
    file_hash VARCHAR(64) NOT NULL CHECK (file_hash ~ '^[0-9a-f]{64}$'),
    valid_rows INTEGER NOT NULL CHECK (valid_rows >= 0),
    invalid_rows INTEGER NOT NULL CHECK (invalid_rows >= 0),
    payload JSONB NOT NULL,
    errors JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ NOT NULL,
    applied_at TIMESTAMPTZ,
    CHECK (expires_at > created_at)
);
CREATE INDEX csv_import_sessions_user_idx ON csv_import_sessions(user_id);
CREATE INDEX csv_import_sessions_expiry_idx ON csv_import_sessions(expires_at);
