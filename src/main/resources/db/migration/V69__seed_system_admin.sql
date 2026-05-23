BEGIN;

SET LOCAL search_path TO prod;

UPDATE users
SET status = 'INACTIVE'::user_status,
    updated_at = now()
WHERE role = 'ADMIN'::user_role
  AND status = 'ACTIVE'::user_status
  AND email <> 'admin@exe.vn';

INSERT INTO users (email,
                   phone,
                   full_name,
                   status,
                   address,
                   age,
                   role,
                   created_at,
                   updated_at)
VALUES ('admin@exe.vn',
        'SYSTEM_ADMIN',
        'System Admin',
        'ACTIVE'::user_status,
        'EXE101',
        0,
        'ADMIN'::user_role,
        now(),
        now()) ON CONFLICT (email) DO
UPDATE SET
    full_name = EXCLUDED.full_name,
    status = 'ACTIVE'::user_status,
    address = EXCLUDED.address,
    age = EXCLUDED.age,
    role = 'ADMIN'::user_role,
    updated_at = now();

INSERT INTO user_credentials (user_id,
                              provider,
                              password_hash,
                              provider_user_id,
                              created_at,
                              updated_at)
SELECT users.id,
       'LOCAL'::credential_provider, '$2a$10$wDvADIiEOzKLeZe/0HNYsO6JW6.lNSZF/4ok2OGaCoZHrZNtqihXm',
       NULL,
       now(),
       now()
FROM users
WHERE users.email = 'admin@exe.vn' ON CONFLICT (user_id) DO
UPDATE SET
    provider = 'LOCAL'::credential_provider,
    password_hash = EXCLUDED.password_hash,
    provider_user_id = NULL,
    updated_at = now();

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_single_active_admin
    ON users ((role))
    WHERE role = 'ADMIN'::user_role
    AND status = 'ACTIVE'::user_status;

COMMIT;
