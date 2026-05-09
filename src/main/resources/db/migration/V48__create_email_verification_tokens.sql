CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    email varchar(255) NOT NULL,

    code varchar(10) NOT NULL,

    purpose varchar(50) NOT NULL,

    verified boolean NOT NULL DEFAULT false,

    expires_at timestamptz NOT NULL,
    used_at timestamptz NULL,

    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_email_verification_tokens_user_purpose_created
    ON email_verification_tokens(user_id, purpose, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_email_verification_tokens_email_purpose_code_active
    ON email_verification_tokens(email, purpose, code)
    WHERE verified = false AND used_at IS NULL;
