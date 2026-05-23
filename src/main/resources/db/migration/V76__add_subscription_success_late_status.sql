DO
$$
BEGIN
  IF
EXISTS (
    SELECT 1
    FROM pg_enum e
    JOIN pg_type t ON t.oid = e.enumtypid
    WHERE t.typname = 'subscription_payment_status'
      AND e.enumlabel = 'LATE_PAYMENT'
  ) THEN
ALTER TYPE subscription_payment_status RENAME VALUE 'LATE_PAYMENT' TO 'SUCCESS_LATE';
ELSIF
NOT EXISTS (
    SELECT 1
    FROM pg_enum e
    JOIN pg_type t ON t.oid = e.enumtypid
    WHERE t.typname = 'subscription_payment_status'
      AND e.enumlabel = 'SUCCESS_LATE'
  ) THEN
ALTER TYPE subscription_payment_status ADD VALUE 'SUCCESS_LATE';
END IF;
END $$;
