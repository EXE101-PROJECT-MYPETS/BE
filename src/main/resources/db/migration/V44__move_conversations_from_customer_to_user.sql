ALTER TABLE conversations
    ADD COLUMN IF NOT EXISTS user_id bigint;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'conversations'
          AND column_name = 'customer_last_read_message_id'
    )
    AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'conversations'
          AND column_name = 'user_last_read_message_id'
    ) THEN
        ALTER TABLE conversations
            RENAME COLUMN customer_last_read_message_id TO user_last_read_message_id;
    ELSIF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'conversations'
          AND column_name = 'user_last_read_message_id'
    ) THEN
        ALTER TABLE conversations
            ADD COLUMN user_last_read_message_id bigint;
    END IF;
END $$;

INSERT INTO users (
    email,
    phone,
    full_name,
    status,
    address,
    age,
    role,
    created_at,
    updated_at
)
SELECT DISTINCT ON (customer.id)
    COALESCE(customer.email, CONCAT('conversation-customer-', customer.id, '@example.local')),
    COALESCE(customer.phone, CONCAT('conversation-customer-', customer.id)),
    customer.full_name,
    'ACTIVE'::user_status,
    NULL,
    0,
    'CUSTOMER'::user_role,
    now(),
    now()
FROM customers customer
JOIN conversations conversation
  ON conversation.shop_id = customer.shop_id
 AND conversation.customer_id = customer.id
WHERE customer.user_id IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM users existing_user
      WHERE (customer.email IS NOT NULL AND existing_user.email = customer.email)
         OR (customer.phone IS NOT NULL AND existing_user.phone = customer.phone)
         OR existing_user.email = CONCAT('conversation-customer-', customer.id, '@example.local')
         OR existing_user.phone = CONCAT('conversation-customer-', customer.id)
  )
ON CONFLICT DO NOTHING;

UPDATE customers customer
SET user_id = matched_user.id
FROM users matched_user
WHERE customer.user_id IS NULL
  AND (
      (customer.email IS NOT NULL AND matched_user.email = customer.email)
      OR (customer.phone IS NOT NULL AND matched_user.phone = customer.phone)
      OR matched_user.email = CONCAT('conversation-customer-', customer.id, '@example.local')
      OR matched_user.phone = CONCAT('conversation-customer-', customer.id)
  )
  AND EXISTS (
      SELECT 1
      FROM conversations conversation
      WHERE conversation.shop_id = customer.shop_id
        AND conversation.customer_id = customer.id
  );

UPDATE conversations conversation
SET user_id = customer.user_id
FROM customers customer
WHERE conversation.user_id IS NULL
  AND conversation.shop_id = customer.shop_id
  AND conversation.customer_id = customer.id;

WITH duplicate_conversations AS (
    SELECT
        id,
        MIN(id) OVER (PARTITION BY shop_id, user_id) AS keep_id
    FROM conversations
    WHERE user_id IS NOT NULL
)
UPDATE messages message
SET conversation_id = duplicate_conversations.keep_id
FROM duplicate_conversations
WHERE message.conversation_id = duplicate_conversations.id
  AND duplicate_conversations.id <> duplicate_conversations.keep_id;

WITH duplicate_conversations AS (
    SELECT
        id,
        MIN(id) OVER (PARTITION BY shop_id, user_id) AS keep_id
    FROM conversations
    WHERE user_id IS NOT NULL
)
DELETE FROM conversations conversation
USING duplicate_conversations
WHERE conversation.id = duplicate_conversations.id
  AND duplicate_conversations.id <> duplicate_conversations.keep_id;

DO $$
DECLARE
    constraint_name text;
BEGIN
    IF to_regclass('messages') IS NOT NULL THEN
        FOR constraint_name IN
            SELECT conname
            FROM pg_constraint
            WHERE conrelid = 'messages'::regclass
              AND contype = 'c'
        LOOP
            EXECUTE format('ALTER TABLE messages DROP CONSTRAINT IF EXISTS %I', constraint_name);
        END LOOP;
    END IF;
END $$;

UPDATE messages message
SET sender_user_id = conversation.user_id
FROM conversations conversation
WHERE message.conversation_id = conversation.id
  AND message.sender_user_id IS NULL
  AND message.sender_type::text IN ('CUSTOMER', 'USER');

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_type type
        JOIN pg_enum enum_value ON enum_value.enumtypid = type.oid
        WHERE type.typname = 'message_sender_type'
          AND enum_value.enumlabel = 'CUSTOMER'
    )
    AND NOT EXISTS (
        SELECT 1
        FROM pg_type type
        JOIN pg_enum enum_value ON enum_value.enumtypid = type.oid
        WHERE type.typname = 'message_sender_type'
          AND enum_value.enumlabel = 'USER'
    ) THEN
        ALTER TYPE message_sender_type RENAME VALUE 'CUSTOMER' TO 'USER';
    END IF;
END $$;

UPDATE messages
SET sender_type = 'USER'::message_sender_type
WHERE sender_type::text = 'CUSTOMER';

ALTER TABLE messages
    DROP COLUMN IF EXISTS sender_customer_id;

ALTER TABLE messages
    ADD CONSTRAINT ck_messages_sender_user
    CHECK (
        sender_user_id IS NOT NULL
        AND sender_type IN ('USER'::message_sender_type, 'SHOP'::message_sender_type)
    );

ALTER TABLE conversations
    DROP CONSTRAINT IF EXISTS fk_conversations_customer_shop,
    DROP CONSTRAINT IF EXISTS uq_conversations_shop_customer,
    DROP CONSTRAINT IF EXISTS conversations_shop_id_customer_id_key;

ALTER TABLE conversations
    ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE conversations
    ADD CONSTRAINT fk_conversations_user
    FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE CASCADE;

ALTER TABLE conversations
    ADD CONSTRAINT uq_conversations_shop_user
    UNIQUE (shop_id, user_id);

ALTER TABLE conversations
    DROP COLUMN IF EXISTS customer_id;
