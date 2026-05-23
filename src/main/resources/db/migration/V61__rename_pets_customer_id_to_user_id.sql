DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'prod'
          AND table_name = 'pets'
          AND column_name = 'customer_id'
    ) THEN
        ALTER TABLE prod.pets
            RENAME COLUMN customer_id TO user_id;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM pg_indexes
        WHERE schemaname = 'prod'
          AND indexname = 'idx_pets_shop_customer'
    ) THEN
        ALTER INDEX prod.idx_pets_shop_customer
            RENAME TO idx_pets_shop_user;
    END IF;
END $$;
