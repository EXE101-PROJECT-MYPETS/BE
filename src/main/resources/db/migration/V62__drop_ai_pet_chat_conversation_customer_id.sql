DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_schema = 'prod'
          AND table_name = 'ai_pet_chat_conversations'
          AND constraint_name = 'fk_ai_pet_chat_conversations_customer'
    ) THEN
        ALTER TABLE prod.ai_pet_chat_conversations
            DROP CONSTRAINT fk_ai_pet_chat_conversations_customer;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM pg_indexes
        WHERE schemaname = 'prod'
          AND indexname = 'idx_ai_pet_chat_conversations_customer'
    ) THEN
        DROP INDEX prod.idx_ai_pet_chat_conversations_customer;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'prod'
          AND table_name = 'ai_pet_chat_conversations'
          AND column_name = 'customer_id'
    ) THEN
        ALTER TABLE prod.ai_pet_chat_conversations
            DROP COLUMN customer_id;
    END IF;
END $$;
