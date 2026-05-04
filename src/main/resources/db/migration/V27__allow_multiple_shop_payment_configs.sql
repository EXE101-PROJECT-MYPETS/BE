ALTER TABLE shop_payment_configs
    DROP CONSTRAINT IF EXISTS shop_payment_configs_shop_id_key;

ALTER TABLE shop_payment_configs
    DROP CONSTRAINT IF EXISTS uq_shop_payment_configs_shop_id;

ALTER TABLE shop_payment_configs
    ADD COLUMN IF NOT EXISTS display_name varchar(100),
    ADD COLUMN IF NOT EXISTS is_default boolean NOT NULL DEFAULT false;

UPDATE shop_payment_configs
SET display_name = account_name
WHERE display_name IS NULL;

WITH default_configs AS (
    SELECT DISTINCT ON (shop_id)
        id
    FROM shop_payment_configs
    WHERE active = true
    ORDER BY shop_id, id
)
UPDATE shop_payment_configs
SET is_default = true
FROM default_configs
WHERE shop_payment_configs.id = default_configs.id;

CREATE INDEX IF NOT EXISTS idx_shop_payment_configs_shop_active
    ON shop_payment_configs(shop_id, active);

CREATE UNIQUE INDEX IF NOT EXISTS uq_shop_payment_configs_shop_bank_account
    ON shop_payment_configs(shop_id, bank_code, account_number);

CREATE UNIQUE INDEX IF NOT EXISTS uq_shop_payment_configs_shop_default
    ON shop_payment_configs(shop_id)
    WHERE is_default = true;

ALTER TABLE shop_payment_configs
    ADD CONSTRAINT chk_shop_payment_configs_display_name_not_blank
        CHECK (display_name IS NULL OR length(btrim(display_name)) > 0);
