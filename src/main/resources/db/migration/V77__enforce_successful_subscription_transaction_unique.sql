CREATE UNIQUE INDEX IF NOT EXISTS uk_subscription_payments_success_provider_transaction
    ON subscription_payments(provider, provider_transaction_id)
    WHERE provider_transaction_id IS NOT NULL
    AND status IN ('SUCCESS'::subscription_payment_status, 'SUCCESS_LATE'::subscription_payment_status);
