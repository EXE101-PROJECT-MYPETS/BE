ALTER TABLE conversations
    ADD COLUMN IF NOT EXISTS shop_last_read_message_id bigint,
    ADD COLUMN IF NOT EXISTS customer_last_read_message_id bigint,
    ADD COLUMN IF NOT EXISTS updated_at timestamptz NOT NULL DEFAULT now();

UPDATE conversations conversation
SET customer_last_read_message_id = member_reads.last_read_message_id
FROM (
    SELECT conversation_id, MAX(last_read_message_id) AS last_read_message_id
    FROM conversation_members
    WHERE member_type = 'CUSTOMER'::conversation_member_type
      AND last_read_message_id IS NOT NULL
    GROUP BY conversation_id
) member_reads
WHERE conversation.id = member_reads.conversation_id;

UPDATE conversations conversation
SET shop_last_read_message_id = member_reads.last_read_message_id
FROM (
    SELECT conversation_id, MAX(last_read_message_id) AS last_read_message_id
    FROM conversation_members
    WHERE member_type = 'STAFF'::conversation_member_type
      AND last_read_message_id IS NOT NULL
    GROUP BY conversation_id
) member_reads
WHERE conversation.id = member_reads.conversation_id;

DROP TRIGGER IF EXISTS trg_conversations_updated_at ON conversations;

CREATE TRIGGER trg_conversations_updated_at
    BEFORE UPDATE ON conversations
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TABLE IF EXISTS conversation_members;

DROP TYPE IF EXISTS conversation_member_type;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_type type
        JOIN pg_enum enum_value ON enum_value.enumtypid = type.oid
        WHERE type.typname = 'message_sender_type'
          AND enum_value.enumlabel = 'STAFF'
    )
    AND NOT EXISTS (
        SELECT 1
        FROM pg_type type
        JOIN pg_enum enum_value ON enum_value.enumtypid = type.oid
        WHERE type.typname = 'message_sender_type'
          AND enum_value.enumlabel = 'SHOP'
    ) THEN
        ALTER TYPE message_sender_type RENAME VALUE 'STAFF' TO 'SHOP';
    END IF;
END $$;
