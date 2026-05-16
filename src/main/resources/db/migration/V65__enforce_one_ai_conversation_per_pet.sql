WITH ranked_conversations AS (SELECT id,
                                     first_value(id) OVER (
            PARTITION BY pet_id
            ORDER BY updated_at DESC NULLS LAST, id DESC
        ) AS keeper_id, row_number() OVER (
            PARTITION BY pet_id
            ORDER BY updated_at DESC NULLS LAST, id DESC
        ) AS row_number
                              FROM prod.ai_pet_chat_conversations)
UPDATE prod.ai_pet_chat_messages message
SET conversation_id = ranked_conversations.keeper_id FROM ranked_conversations
WHERE message.conversation_id = ranked_conversations.id
  AND ranked_conversations.row_number
    > 1;

WITH ranked_conversations AS (SELECT id,
                                     row_number() OVER (
            PARTITION BY pet_id
            ORDER BY updated_at DESC NULLS LAST, id DESC
        ) AS row_number
                              FROM prod.ai_pet_chat_conversations)
DELETE
FROM prod.ai_pet_chat_conversations conversation USING ranked_conversations
WHERE conversation.id = ranked_conversations.id
  AND ranked_conversations.row_number
    > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uq_ai_pet_chat_conversations_pet_id
    ON prod.ai_pet_chat_conversations(pet_id);
