ALTER TABLE prod.user_credentials
    ADD COLUMN IF NOT EXISTS provider_user_id varchar(255);
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_credentials_provider_uid
    ON prod.user_credentials(provider, provider_user_id)
    WHERE provider_user_id IS NOT NULL;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_type t
    JOIN pg_namespace n ON n.oid = t.typnamespace
    WHERE t.typname = 'user_role' AND n.nspname = 'prod'
  ) THEN
CREATE TYPE prod.user_role AS ENUM ('ADMIN', 'STAFF', 'CUSTOMER', 'SHOP');
END IF;
END $$;
ALTER TABLE prod.users
    ADD COLUMN role prod.user_role NOT NULL DEFAULT 'CUSTOMER';