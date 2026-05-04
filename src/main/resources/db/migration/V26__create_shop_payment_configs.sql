CREATE TABLE IF NOT EXISTS shop_payment_configs (
    id bigserial PRIMARY KEY,

    shop_id bigint NOT NULL UNIQUE REFERENCES shops(id) ON DELETE CASCADE,

    bank_code varchar(50) NOT NULL,
    account_number varchar(100) NOT NULL,
    account_name varchar(255) NOT NULL,

    active boolean NOT NULL DEFAULT true,

    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT chk_shop_payment_configs_bank_code_not_blank
        CHECK (length(btrim(bank_code)) > 0),
    CONSTRAINT chk_shop_payment_configs_account_number_not_blank
        CHECK (length(btrim(account_number)) > 0),
    CONSTRAINT chk_shop_payment_configs_account_name_not_blank
        CHECK (length(btrim(account_name)) > 0)
);

DROP TRIGGER IF EXISTS trg_shop_payment_configs_updated_at ON shop_payment_configs;

CREATE TRIGGER trg_shop_payment_configs_updated_at
    BEFORE UPDATE ON shop_payment_configs
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
