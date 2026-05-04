DROP INDEX IF EXISTS uq_shop_payment_configs_shop_default;

ALTER TABLE shop_payment_configs
    DROP COLUMN IF EXISTS is_default;
