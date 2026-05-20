ALTER TABLE subscription_payments
    ADD COLUMN IF NOT EXISTS duration_months int NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS duration_days int NOT NULL DEFAULT 30;

UPDATE subscription_payments
SET duration_months = 1
WHERE duration_months IS NULL;

UPDATE subscription_payments
SET duration_days = 30
WHERE duration_days IS NULL;

ALTER TABLE subscription_payments
    ADD CONSTRAINT chk_subscription_payments_duration_months_valid
        CHECK (duration_months IN (1, 3, 6)),
    ADD CONSTRAINT chk_subscription_payments_duration_days_positive
        CHECK (duration_days > 0);
