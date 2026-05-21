DO
$$
BEGIN
  IF
EXISTS (
    SELECT 1
    FROM pg_enum e
    JOIN pg_type t ON t.oid = e.enumtypid
    WHERE t.typname = 'subscription_payment_status'
      AND e.enumlabel = 'PAID'
  ) THEN
ALTER TYPE subscription_payment_status RENAME VALUE 'PAID' TO 'SUCCESS';
ELSIF
NOT EXISTS (
    SELECT 1
    FROM pg_enum e
    JOIN pg_type t ON t.oid = e.enumtypid
    WHERE t.typname = 'subscription_payment_status'
      AND e.enumlabel = 'SUCCESS'
  ) THEN
ALTER TYPE subscription_payment_status ADD VALUE 'SUCCESS';
END IF;

  IF
EXISTS (
    SELECT 1
    FROM pg_enum e
    JOIN pg_type t ON t.oid = e.enumtypid
    WHERE t.typname = 'subscription_payment_status'
      AND e.enumlabel = 'CANCELLED'
  ) THEN
ALTER TYPE subscription_payment_status RENAME VALUE 'CANCELLED' TO 'CANCELED';
ELSIF
NOT EXISTS (
    SELECT 1
    FROM pg_enum e
    JOIN pg_type t ON t.oid = e.enumtypid
    WHERE t.typname = 'subscription_payment_status'
      AND e.enumlabel = 'CANCELED'
  ) THEN
ALTER TYPE subscription_payment_status ADD VALUE 'CANCELED';
END IF;

  IF
NOT EXISTS (
    SELECT 1
    FROM pg_enum e
    JOIN pg_type t ON t.oid = e.enumtypid
    WHERE t.typname = 'subscription_payment_status'
      AND e.enumlabel = 'EXPIRED'
  ) THEN
ALTER TYPE subscription_payment_status ADD VALUE 'EXPIRED';
END IF;

  IF
EXISTS (
    SELECT 1
    FROM pg_enum e
    JOIN pg_type t ON t.oid = e.enumtypid
    WHERE t.typname = 'shop_subscription_status'
      AND e.enumlabel = 'CANCELLED'
  ) THEN
ALTER TYPE shop_subscription_status RENAME VALUE 'CANCELLED' TO 'CANCELED';
ELSIF
NOT EXISTS (
    SELECT 1
    FROM pg_enum e
    JOIN pg_type t ON t.oid = e.enumtypid
    WHERE t.typname = 'shop_subscription_status'
      AND e.enumlabel = 'CANCELED'
  ) THEN
ALTER TYPE shop_subscription_status ADD VALUE 'CANCELED';
END IF;
END $$;

ALTER TABLE shop_subscriptions
    ADD COLUMN IF NOT EXISTS plan_type varchar (20) NOT NULL DEFAULT 'TRIAL',
    ADD COLUMN IF NOT EXISTS expired_at timestamptz;

UPDATE shop_subscriptions
SET expired_at = current_period_end
WHERE expired_at IS NULL;

ALTER TABLE shop_subscriptions
    ALTER COLUMN expired_at SET NOT NULL;

ALTER TABLE subscription_payments
    ADD COLUMN IF NOT EXISTS plan_code varchar (50) NOT NULL DEFAULT 'MONTHLY',
    ADD COLUMN IF NOT EXISTS provider varchar (50) NOT NULL DEFAULT 'SEPAY',
    ADD COLUMN IF NOT EXISTS transfer_content varchar (100),
    ADD COLUMN IF NOT EXISTS provider_transaction_id varchar (100),
    ADD COLUMN IF NOT EXISTS raw_payload text,
    ADD COLUMN IF NOT EXISTS expired_at timestamptz;

UPDATE subscription_payments
SET transfer_content = invoice_number
WHERE transfer_content IS NULL;

UPDATE subscription_payments
SET expired_at = created_at + interval '15 minutes'
WHERE expired_at IS NULL;

ALTER TABLE subscription_payments
    ALTER COLUMN transfer_content SET NOT NULL,
ALTER
COLUMN expired_at SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_subscription_payments_provider_transaction
    ON subscription_payments(provider, provider_transaction_id)
    WHERE provider_transaction_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_subscription_payments_pending_expired
    ON subscription_payments(status, expired_at);
