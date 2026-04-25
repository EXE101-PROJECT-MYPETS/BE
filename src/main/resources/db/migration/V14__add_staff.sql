BEGIN;

SET LOCAL search_path TO prod;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM shops WHERE id = 1) THEN
        RAISE EXCEPTION 'Shop id 1 does not exist';
END IF;
END $$;

CREATE TEMP TABLE seed_shop_1_staff (
    email varchar(255) PRIMARY KEY,
    phone varchar(50) NOT NULL,
    full_name varchar(255) NOT NULL,
    age int NOT NULL,
    address varchar(255)
) ON COMMIT DROP;

INSERT INTO seed_shop_1_staff (email, phone, full_name, age, address)
VALUES
    ('staff.shop1.01@example.com', '0911000001', 'Shop 1 Staff 01', 24, 'Shop 1'),
    ('staff.shop1.02@example.com', '0911000002', 'Shop 1 Staff 02', 25, 'Shop 1'),
    ('staff.shop1.03@example.com', '0911000003', 'Shop 1 Staff 03', 26, 'Shop 1');

-- Password for all seeded staff accounts: 123456
INSERT INTO users (
    email,
    phone,
    full_name,
    status,
    address,
    age,
    role,
    created_at,
    updated_at
)
SELECT
    seed.email,
    seed.phone,
    seed.full_name,
    'ACTIVE'::user_status,
    seed.address,
    seed.age,
    'SHOP'::user_role,
    now(),
    now()
FROM seed_shop_1_staff seed
    ON CONFLICT (email) DO UPDATE SET
    phone = EXCLUDED.phone,
                               full_name = EXCLUDED.full_name,
                               status = 'ACTIVE'::user_status,
                               address = EXCLUDED.address,
                               age = EXCLUDED.age,
                               role = 'SHOP'::user_role,
                               updated_at = now();

INSERT INTO user_credentials (
    user_id,
    provider,
    password_hash,
    provider_user_id,
    created_at,
    updated_at
)
SELECT
    users.id,
    'LOCAL'::credential_provider,
    '$2a$10$c0zDM/R/EIaH/emGTzoqSuJNSg1.4XZuSkVlZGDt4p6TAn8ER/Aya',
    NULL,
    now(),
    now()
FROM users
         JOIN seed_shop_1_staff seed ON seed.email = users.email
    ON CONFLICT (user_id) DO UPDATE SET
    provider = 'LOCAL'::credential_provider,
                                 password_hash = EXCLUDED.password_hash,
                                 provider_user_id = NULL,
                                 updated_at = now();

INSERT INTO shop_members (
    shop_id,
    user_id,
    role,
    status
)
SELECT
    1,
    users.id,
    'STAFF'::shop_role,
    'ACTIVE'::member_status
FROM users
         JOIN seed_shop_1_staff seed ON seed.email = users.email
    ON CONFLICT (shop_id, user_id) DO UPDATE SET
    role = 'STAFF'::shop_role,
                                          status = 'ACTIVE'::member_status;

COMMIT;
