ALTER TYPE subscription_payment_method ADD VALUE IF NOT EXISTS 'SEPAY';

ALTER TABLE subscription_payments
    ADD COLUMN IF NOT EXISTS invoice_number varchar (50),
    ADD COLUMN IF NOT EXISTS checkout_url text;

UPDATE subscription_payments
SET invoice_number = CONCAT('SUB_', id)
WHERE invoice_number IS NULL;

ALTER TABLE subscription_payments
    ALTER COLUMN invoice_number SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_subscription_payments_invoice_number
    ON subscription_payments(invoice_number);
