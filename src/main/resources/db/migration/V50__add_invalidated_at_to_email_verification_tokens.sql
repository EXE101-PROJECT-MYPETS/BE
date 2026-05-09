ALTER TABLE email_verification_tokens
    ADD COLUMN IF NOT EXISTS invalidated_at timestamptz NULL;

CREATE INDEX IF NOT EXISTS idx_email_verification_tokens_email_purpose_active
    ON email_verification_tokens(email, purpose, created_at DESC)
    WHERE verified = false AND used_at IS NULL AND invalidated_at IS NULL;