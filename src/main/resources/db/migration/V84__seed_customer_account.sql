BEGIN;

SET LOCAL search_path TO prod;

-- Password: 123456
INSERT INTO users (email,
                   phone,
                   full_name,
                   status,
                   address,
                   age,
                   role,
                   created_at,
                   updated_at)
VALUES ('customer@gmail.com',
        '0901234567',
        'Customer Demo',
        'ACTIVE'::user_status,
        'Ho Chi Minh City',
        24,
        'CUSTOMER'::user_role,
        now(),
        now()) ON CONFLICT (email) DO
UPDATE SET
    phone = EXCLUDED.phone,
    full_name = EXCLUDED.full_name,
    status = 'ACTIVE'::user_status,
    address = EXCLUDED.address,
    age = EXCLUDED.age,
    role = 'CUSTOMER'::user_role,
    updated_at = now();

INSERT INTO user_credentials (user_id,
                              provider,
                              password_hash,
                              provider_user_id,
                              created_at,
                              updated_at)
SELECT u.id,
       'LOCAL'::credential_provider, '$2a$10$c0zDM/R/EIaH/emGTzoqSuJNSg1.4XZuSkVlZGDt4p6TAn8ER/Aya',
       NULL,
       now(),
       now()
FROM users u
WHERE u.email = 'customer@gmail.com' ON CONFLICT (user_id) DO
UPDATE SET
    provider = 'LOCAL'::credential_provider,
    password_hash = EXCLUDED.password_hash,
    provider_user_id = NULL,
    updated_at = now();

COMMIT;
