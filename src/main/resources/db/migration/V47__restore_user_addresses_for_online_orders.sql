CREATE TABLE IF NOT EXISTS user_addresses (
    id bigserial PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    name varchar(255) NOT NULL,
    tel varchar(30) NOT NULL,
    address text NOT NULL,
    province varchar(100) NOT NULL,
    district varchar(100) NOT NULL,
    ward varchar(100) NOT NULL,
    hamlet varchar(255) NOT NULL,
    is_default boolean NOT NULL DEFAULT false,

    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT chk_user_addresses_name_not_blank
        CHECK (length(btrim(name)) > 0),
    CONSTRAINT chk_user_addresses_tel_not_blank
        CHECK (length(btrim(tel)) > 0),
    CONSTRAINT chk_user_addresses_address_not_blank
        CHECK (length(btrim(address)) > 0),
    CONSTRAINT chk_user_addresses_province_not_blank
        CHECK (length(btrim(province)) > 0),
    CONSTRAINT chk_user_addresses_district_not_blank
        CHECK (length(btrim(district)) > 0),
    CONSTRAINT chk_user_addresses_ward_not_blank
        CHECK (length(btrim(ward)) > 0),
    CONSTRAINT chk_user_addresses_hamlet_not_blank
        CHECK (length(btrim(hamlet)) > 0)
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_user_addresses_updated_at') THEN
        CREATE TRIGGER trg_user_addresses_updated_at
            BEFORE UPDATE ON user_addresses
            FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_user_addresses_user_created
    ON user_addresses(user_id, created_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS uq_user_addresses_user_default
    ON user_addresses(user_id)
    WHERE is_default;

INSERT INTO user_addresses (
    user_id,
    name,
    tel,
    address,
    province,
    district,
    ward,
    hamlet,
    is_default
)
SELECT
    users.id,
    COALESCE(NULLIF(btrim(users.full_name), ''), 'Nguoi nhan ' || users.id),
    COALESCE(NULLIF(btrim(users.phone), ''), '090' || lpad(users.id::text, 7, '0')),
    COALESCE(NULLIF(btrim(users.address), ''), 'Chua cap nhat dia chi'),
    'TP. Ho Chi Minh',
    'Quan 1',
    'Phuong Ben Nghe',
    'Khac',
    true
FROM users
WHERE NOT EXISTS (
    SELECT 1
    FROM user_addresses existing_address
    WHERE existing_address.user_id = users.id
);

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS user_address_id bigint REFERENCES user_addresses(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_orders_user_address_id
    ON orders(user_address_id);
