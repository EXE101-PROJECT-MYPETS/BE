DO
$$
BEGIN
    IF
NOT EXISTS (
        SELECT 1
        FROM pg_enum e
                 JOIN pg_type t ON e.enumtypid = t.oid
        WHERE t.typname = 'subscription_payment_status'
          AND e.enumlabel = 'PAID_AFTER_CANCEL'
    ) THEN
ALTER TYPE subscription_payment_status ADD VALUE 'PAID_AFTER_CANCEL';
END IF;
END $$;
