-- Align pets model for user-owned profile (independent from one shop)
-- and support many shops per pet through link table.

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'pets'
          AND column_name = 'user_id'
    ) THEN
        ALTER TABLE pets ADD COLUMN user_id bigint;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'pets'
          AND column_name = 'avatar_url'
    ) THEN
        ALTER TABLE pets ADD COLUMN avatar_url text;
    END IF;
END $$;

-- Backfill pets.user_id from customers.user_id when possible.
UPDATE pets p
SET user_id = c.user_id
FROM customers c
WHERE p.customer_id = c.id
  AND p.user_id IS NULL
  AND c.user_id IS NOT NULL;

-- Customer can be null for personal pets that are not linked to any shop yet.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'pets'
          AND column_name = 'customer_id'
          AND is_nullable = 'NO'
    ) THEN
        ALTER TABLE pets ALTER COLUMN customer_id DROP NOT NULL;
    END IF;
END $$;

-- shop_id can be null in schemas where this column exists.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'pets'
          AND column_name = 'shop_id'
          AND is_nullable = 'NO'
    ) THEN
        ALTER TABLE pets ALTER COLUMN shop_id DROP NOT NULL;
    END IF;
END $$;

-- FK pets.user_id -> users(id)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_schema = current_schema()
          AND table_name = 'pets'
          AND constraint_name = 'fk_pets_user'
    ) THEN
        ALTER TABLE pets
            ADD CONSTRAINT fk_pets_user
                FOREIGN KEY (user_id)
                    REFERENCES users(id)
                    ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_pets_user_id ON pets(user_id);

CREATE TABLE IF NOT EXISTS pet_shop_links (
    pet_id bigint NOT NULL REFERENCES pets(id) ON DELETE CASCADE,
    shop_id bigint NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    linked_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (pet_id, shop_id)
);

CREATE INDEX IF NOT EXISTS idx_pet_shop_links_shop_id ON pet_shop_links(shop_id);

-- Backfill links from existing pets.shop_id if that column exists and has data.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'pets'
          AND column_name = 'shop_id'
    ) THEN
        INSERT INTO pet_shop_links (pet_id, shop_id)
        SELECT id, shop_id
        FROM pets
        WHERE shop_id IS NOT NULL
        ON CONFLICT (pet_id, shop_id) DO NOTHING;
    END IF;
END $$;
