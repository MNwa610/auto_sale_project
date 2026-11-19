-- FK actor_id может стать NULL после удаления пользователя; снимок логина остаётся.
-- Старые события не переписываются: неизвестный исторический логин остаётся NULL.
ALTER TABLE audit_events ADD COLUMN actor_login VARCHAR(50);
ALTER TABLE audit_events ADD CONSTRAINT audit_details_object CHECK (jsonb_typeof(details) = 'object');
