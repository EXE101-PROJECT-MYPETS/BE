DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'commission_source_type') THEN
            CREATE TYPE commission_source_type AS ENUM ('ORDER', 'SERVICE_BOOKING', 'VET_BOOKING');
        END IF;

        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'commission_status') THEN
            CREATE TYPE commission_status AS ENUM ('PENDING', 'INVOICED', 'COLLECTED', 'REFUNDED', 'CANCELED');
        END IF;

        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'commission_invoice_status') THEN
            CREATE TYPE commission_invoice_status AS ENUM ('PENDING', 'PAID', 'OVERDUE', 'CANCELED');
        END IF;
    END
$$;

CREATE TABLE IF NOT EXISTS platform_commissions
(
    id                  bigserial PRIMARY KEY,
    shop_id             bigint                 NOT NULL REFERENCES shops (id) ON DELETE CASCADE,
    source_type         commission_source_type NOT NULL,
    source_id           bigint                 NOT NULL,
    gross_amount        bigint                 NOT NULL DEFAULT 0,
    discount_amount     bigint                 NOT NULL DEFAULT 0,
    shipping_fee        bigint                 NOT NULL DEFAULT 0,
    commission_base     bigint                 NOT NULL DEFAULT 0,
    commission_rate_bps int                    NOT NULL DEFAULT 1500,
    commission_amount   bigint                 NOT NULL DEFAULT 0,
    status              commission_status      NOT NULL DEFAULT 'PENDING',
    created_at          timestamptz            NOT NULL DEFAULT now(),
    invoiced_at         timestamptz,
    collected_at        timestamptz,
    refunded_at         timestamptz,
    CONSTRAINT uq_platform_commissions_source UNIQUE (source_type, source_id),
    CONSTRAINT chk_platform_commissions_amounts_non_negative CHECK (
        gross_amount >= 0
            AND discount_amount >= 0
            AND shipping_fee >= 0
            AND commission_base >= 0
            AND commission_rate_bps >= 0
            AND commission_amount >= 0
        )
);

CREATE INDEX IF NOT EXISTS idx_platform_commissions_shop_status_created
    ON platform_commissions (shop_id, status, created_at);

CREATE INDEX IF NOT EXISTS idx_platform_commissions_status_created
    ON platform_commissions (status, created_at);

CREATE TABLE IF NOT EXISTS platform_commission_invoices
(
    id                      bigserial PRIMARY KEY,
    shop_id                 bigint                    NOT NULL REFERENCES shops (id) ON DELETE CASCADE,
    invoice_code            varchar(50)               NOT NULL UNIQUE,
    period_from             date                      NOT NULL,
    period_to               date                      NOT NULL,
    total_gross_amount      bigint                    NOT NULL DEFAULT 0,
    total_commission_amount bigint                    NOT NULL DEFAULT 0,
    status                  commission_invoice_status NOT NULL DEFAULT 'PENDING',
    bank_code               varchar(50),
    account_number          varchar(100),
    account_name            varchar(255),
    transfer_content        varchar(100)              NOT NULL,
    qr_url                  text,
    created_at              timestamptz               NOT NULL DEFAULT now(),
    due_at                  timestamptz               NOT NULL,
    paid_at                 timestamptz,
    CONSTRAINT uq_platform_commission_invoices_shop_period UNIQUE (shop_id, period_from, period_to),
    CONSTRAINT chk_platform_commission_invoices_period_valid CHECK (period_to >= period_from),
    CONSTRAINT chk_platform_commission_invoices_amounts_non_negative CHECK (
        total_gross_amount >= 0
            AND total_commission_amount >= 0
        )
);

CREATE INDEX IF NOT EXISTS idx_platform_commission_invoices_shop_status_due
    ON platform_commission_invoices (shop_id, status, due_at);

CREATE INDEX IF NOT EXISTS idx_platform_commission_invoices_status_due
    ON platform_commission_invoices (status, due_at);

CREATE TABLE IF NOT EXISTS platform_commission_invoice_items
(
    id                bigserial PRIMARY KEY,
    invoice_id        bigint      NOT NULL REFERENCES platform_commission_invoices (id) ON DELETE CASCADE,
    commission_id     bigint      NOT NULL REFERENCES platform_commissions (id),
    commission_amount bigint      NOT NULL DEFAULT 0,
    created_at        timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_platform_commission_invoice_items_commission UNIQUE (commission_id),
    CONSTRAINT chk_platform_commission_invoice_items_amount_non_negative CHECK (commission_amount >= 0)
);

CREATE INDEX IF NOT EXISTS idx_platform_commission_invoice_items_invoice
    ON platform_commission_invoice_items (invoice_id);
