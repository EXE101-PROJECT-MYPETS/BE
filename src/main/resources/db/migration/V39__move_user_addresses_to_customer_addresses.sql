CREATE TABLE IF NOT EXISTS customer_addresses (
    id bigserial PRIMARY KEY,
    customer_id bigint NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    source_user_address_id bigint,

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

    CONSTRAINT chk_customer_addresses_name_not_blank
        CHECK (length(btrim(name)) > 0),
    CONSTRAINT chk_customer_addresses_tel_not_blank
        CHECK (length(btrim(tel)) > 0),
    CONSTRAINT chk_customer_addresses_address_not_blank
        CHECK (length(btrim(address)) > 0),
    CONSTRAINT chk_customer_addresses_province_not_blank
        CHECK (length(btrim(province)) > 0),
    CONSTRAINT chk_customer_addresses_district_not_blank
        CHECK (length(btrim(district)) > 0),
    CONSTRAINT chk_customer_addresses_ward_not_blank
        CHECK (length(btrim(ward)) > 0),
    CONSTRAINT chk_customer_addresses_hamlet_not_blank
        CHECK (length(btrim(hamlet)) > 0)
);

ALTER TABLE customer_addresses
    ADD COLUMN IF NOT EXISTS source_user_address_id bigint;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_customer_addresses_updated_at') THEN
        CREATE TRIGGER trg_customer_addresses_updated_at
            BEFORE UPDATE ON customer_addresses
            FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_customer_addresses_customer_created
    ON customer_addresses(customer_id, created_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS uq_customer_addresses_customer_default
    ON customer_addresses(customer_id)
    WHERE is_default;

INSERT INTO customer_addresses (
    customer_id,
    source_user_address_id,
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
    customers.id,
    user_addresses.id,
    user_addresses.name,
    user_addresses.tel,
    user_addresses.address,
    user_addresses.province,
    user_addresses.district,
    user_addresses.ward,
    user_addresses.hamlet,
    user_addresses.is_default
FROM customers
JOIN user_addresses
    ON user_addresses.user_id = customers.user_id
WHERE NOT EXISTS (
    SELECT 1
    FROM customer_addresses existing_address
    WHERE existing_address.customer_id = customers.id
      AND existing_address.source_user_address_id = user_addresses.id
);

INSERT INTO customer_addresses (
    customer_id,
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
    customers.id,
    COALESCE(NULLIF(btrim(customers.full_name), ''), 'Nguoi nhan ' || customers.id),
    COALESCE(NULLIF(btrim(customers.phone), ''), '090' || lpad(customers.id::text, 7, '0')),
    CASE (customers.id % 6)
        WHEN 0 THEN '123 Nguyen Chi Thanh'
        WHEN 1 THEN '45 Le Loi'
        WHEN 2 THEN '78 Nguyen Hue'
        WHEN 3 THEN '12 Cach Mang Thang 8'
        WHEN 4 THEN '89 Vo Van Tan'
        ELSE '56 Dien Bien Phu'
    END,
    'TP. Ho Chi Minh',
    CASE (customers.id % 6)
        WHEN 0 THEN 'Quan 1'
        WHEN 1 THEN 'Quan 3'
        WHEN 2 THEN 'Quan Binh Thanh'
        WHEN 3 THEN 'Quan 10'
        WHEN 4 THEN 'Quan Phu Nhuan'
        ELSE 'Thanh pho Thu Duc'
    END,
    CASE (customers.id % 6)
        WHEN 0 THEN 'Phuong Ben Nghe'
        WHEN 1 THEN 'Phuong Vo Thi Sau'
        WHEN 2 THEN 'Phuong 25'
        WHEN 3 THEN 'Phuong 12'
        WHEN 4 THEN 'Phuong 7'
        ELSE 'Phuong Thao Dien'
    END,
    'Khac',
    true
FROM customers
WHERE NOT EXISTS (
    SELECT 1
    FROM customer_addresses existing_address
    WHERE existing_address.customer_id = customers.id
);

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS customer_address_id bigint REFERENCES customer_addresses(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_orders_customer_address_id
    ON orders(customer_address_id);

UPDATE orders
SET customer_address_id = customer_addresses.id
FROM customer_addresses
WHERE orders.customer_id = customer_addresses.customer_id
  AND orders.user_address_id = customer_addresses.source_user_address_id
  AND orders.user_address_id IS NOT NULL
  AND orders.customer_address_id IS NULL;

UPDATE orders
SET customer_address_id = customer_addresses.id
FROM customer_addresses
WHERE orders.customer_id = customer_addresses.customer_id
  AND customer_addresses.is_default = true
  AND orders.customer_address_id IS NULL;

DROP INDEX IF EXISTS idx_orders_user_address_id;
ALTER TABLE orders DROP COLUMN IF EXISTS user_address_id;

ALTER TABLE customer_addresses DROP COLUMN IF EXISTS source_user_address_id;

DROP TABLE IF EXISTS user_addresses;
