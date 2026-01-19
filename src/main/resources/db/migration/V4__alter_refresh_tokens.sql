ALTER TABLE refresh_tokens
    RENAME COLUMN token_hash TO token;

ALTER TABLE refresh_tokens
    RENAME COLUMN expires_at TO expiry_at;