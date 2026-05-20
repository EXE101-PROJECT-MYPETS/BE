DO
$$
BEGIN
  IF
NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'shop_subscription_status') THEN
CREATE TYPE shop_subscription_status AS ENUM ('TRIALING', 'ACTIVE', 'EXPIRED', 'CANCELLED');
END IF;

  IF
NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'subscription_payment_status') THEN
CREATE TYPE subscription_payment_status AS ENUM ('PENDING', 'PAID', 'FAILED', 'CANCELLED');
END IF;

  IF
NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'subscription_payment_method') THEN
CREATE TYPE subscription_payment_method AS ENUM ('BANK_TRANSFER', 'VNPAY', 'MOMO', 'MANUAL');
END IF;
END $$;

CREATE TABLE IF NOT EXISTS subscription_plans
(
    id
    bigserial
    PRIMARY
    KEY,
    code
    varchar
(
    50
) NOT NULL UNIQUE,
    name varchar
(
    255
) NOT NULL,
    duration_months int NOT NULL,
    price bigint NOT NULL,
    active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now
(
),
    updated_at timestamptz NOT NULL DEFAULT now
(
),
    CONSTRAINT chk_subscription_plans_duration_positive CHECK
(
    duration_months >
    0
),
    CONSTRAINT chk_subscription_plans_price_non_negative CHECK
(
    price
    >=
    0
)
    );

CREATE TRIGGER trg_subscription_plans_updated_at
    BEFORE UPDATE
    ON subscription_plans
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE IF NOT EXISTS shop_subscriptions
(
    id
    bigserial
    PRIMARY
    KEY,
    shop_id
    bigint
    NOT
    NULL
    UNIQUE,
    plan_id
    bigint,
    status
    shop_subscription_status
    NOT
    NULL
    DEFAULT
    'TRIALING',
    started_at
    timestamptz
    NOT
    NULL
    DEFAULT
    now
(
),
    trial_ends_at timestamptz,
    current_period_start timestamptz NOT NULL DEFAULT now
(
),
    current_period_end timestamptz NOT NULL,
    cancelled_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now
(
),
    updated_at timestamptz NOT NULL DEFAULT now
(
),
    CONSTRAINT fk_shop_subscriptions_shop
    FOREIGN KEY
(
    shop_id
) REFERENCES shops
(
    id
) ON DELETE CASCADE,
    CONSTRAINT fk_shop_subscriptions_plan
    FOREIGN KEY
(
    plan_id
) REFERENCES subscription_plans
(
    id
),
    CONSTRAINT chk_shop_subscriptions_period_valid
    CHECK
(
    current_period_end >
    current_period_start
),
    CONSTRAINT chk_shop_subscriptions_trial_valid
    CHECK
(
    trial_ends_at
    IS
    NULL
    OR
    trial_ends_at
    >=
    started_at
)
    );

CREATE INDEX IF NOT EXISTS idx_shop_subscriptions_status
    ON shop_subscriptions(status);

CREATE INDEX IF NOT EXISTS idx_shop_subscriptions_period_end
    ON shop_subscriptions(current_period_end);

CREATE TRIGGER trg_shop_subscriptions_updated_at
    BEFORE UPDATE
    ON shop_subscriptions
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE IF NOT EXISTS subscription_payments
(
    id
    bigserial
    PRIMARY
    KEY,
    shop_id
    bigint
    NOT
    NULL,
    subscription_id
    bigint
    NOT
    NULL,
    plan_id
    bigint
    NOT
    NULL,
    amount
    bigint
    NOT
    NULL,
    status
    subscription_payment_status
    NOT
    NULL
    DEFAULT
    'PENDING',
    payment_method
    subscription_payment_method
    NOT
    NULL
    DEFAULT
    'BANK_TRANSFER',
    period_start
    timestamptz
    NOT
    NULL,
    period_end
    timestamptz
    NOT
    NULL,
    paid_at
    timestamptz,
    created_at
    timestamptz
    NOT
    NULL
    DEFAULT
    now
(
),
    updated_at timestamptz NOT NULL DEFAULT now
(
),
    CONSTRAINT fk_subscription_payments_shop
    FOREIGN KEY
(
    shop_id
) REFERENCES shops
(
    id
) ON DELETE CASCADE,
    CONSTRAINT fk_subscription_payments_subscription
    FOREIGN KEY
(
    subscription_id
) REFERENCES shop_subscriptions
(
    id
)
  ON DELETE CASCADE,
    CONSTRAINT fk_subscription_payments_plan
    FOREIGN KEY
(
    plan_id
) REFERENCES subscription_plans
(
    id
),
    CONSTRAINT chk_subscription_payments_amount_non_negative CHECK
(
    amount
    >=
    0
),
    CONSTRAINT chk_subscription_payments_period_valid CHECK
(
    period_end >
    period_start
)
    );

CREATE INDEX IF NOT EXISTS idx_subscription_payments_shop_created
    ON subscription_payments(shop_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_subscription_payments_subscription
    ON subscription_payments(subscription_id);

CREATE INDEX IF NOT EXISTS idx_subscription_payments_status
    ON subscription_payments(status);

CREATE TRIGGER trg_subscription_payments_updated_at
    BEFORE UPDATE
    ON subscription_payments
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

INSERT INTO subscription_plans (code, name, duration_months, price, active)
VALUES ('BASIC_1_MONTH', 'Goi 1 thang', 1, 500000, true),
       ('BASIC_3_MONTHS', 'Goi 3 thang', 3, 1500000, true),
       ('BASIC_6_MONTHS', 'Goi 6 thang', 6, 3000000, true) ON CONFLICT (code) DO
UPDATE
    SET name = EXCLUDED.name,
    duration_months = EXCLUDED.duration_months,
    price = EXCLUDED.price,
    active = EXCLUDED.active,
    updated_at = now();
