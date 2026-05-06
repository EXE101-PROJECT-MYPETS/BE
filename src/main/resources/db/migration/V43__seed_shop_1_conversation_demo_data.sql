WITH seed_customers AS (
    SELECT customers.id AS customer_id, customers.phone
    FROM customers
    WHERE customers.shop_id = 1
      AND customers.phone IN ('096778899', '0987654321', '0901122334')
),
inserted_conversations AS (
    INSERT INTO conversations (shop_id, customer_id, created_at, updated_at)
    SELECT
        1,
        seed_customers.customer_id,
        now() - CASE seed_customers.phone
            WHEN '096778899' THEN interval '2 hours'
            WHEN '0987654321' THEN interval '90 minutes'
            ELSE interval '45 minutes'
        END,
        now()
    FROM seed_customers
    ON CONFLICT (shop_id, customer_id) DO UPDATE SET
        updated_at = EXCLUDED.updated_at
    RETURNING id, shop_id, customer_id
),
shop_account AS (
    SELECT shop_members.user_id
    FROM shop_members
    WHERE shop_members.shop_id = 1
      AND shop_members.status = 'ACTIVE'::member_status
    ORDER BY shop_members.created_at ASC, shop_members.user_id ASC
    LIMIT 1
),
seed_messages AS (
    SELECT
        inserted_conversations.id AS conversation_id,
        inserted_conversations.shop_id,
        'CUSTOMER'::message_sender_type AS sender_type,
        inserted_conversations.customer_id AS sender_customer_id,
        NULL::bigint AS sender_user_id,
        'Em cần tư vấn dịch vụ grooming cho bé cún.' AS body,
        now() - interval '30 minutes' AS created_at
    FROM inserted_conversations
    JOIN seed_customers ON seed_customers.customer_id = inserted_conversations.customer_id
    WHERE seed_customers.phone = '096778899'

    UNION ALL

    SELECT
        inserted_conversations.id,
        inserted_conversations.shop_id,
        'SHOP'::message_sender_type,
        NULL::bigint,
        shop_account.user_id,
        'Shop chào bạn, bé khoảng bao nhiêu kg để shop tư vấn gói phù hợp ạ?',
        now() - interval '28 minutes'
    FROM inserted_conversations
    JOIN seed_customers ON seed_customers.customer_id = inserted_conversations.customer_id
    CROSS JOIN shop_account
    WHERE seed_customers.phone = '096778899'

    UNION ALL

    SELECT
        inserted_conversations.id,
        inserted_conversations.shop_id,
        'CUSTOMER'::message_sender_type,
        inserted_conversations.customer_id,
        NULL::bigint,
        'Bé nhà em 5kg, lông hơi rối.',
        now() - interval '24 minutes'
    FROM inserted_conversations
    JOIN seed_customers ON seed_customers.customer_id = inserted_conversations.customer_id
    WHERE seed_customers.phone = '096778899'

    UNION ALL

    SELECT
        inserted_conversations.id,
        inserted_conversations.shop_id,
        'CUSTOMER'::message_sender_type,
        inserted_conversations.customer_id,
        NULL::bigint,
        'Shop còn lịch tắm spa chiều nay không?',
        now() - interval '18 minutes'
    FROM inserted_conversations
    JOIN seed_customers ON seed_customers.customer_id = inserted_conversations.customer_id
    WHERE seed_customers.phone = '0987654321'

    UNION ALL

    SELECT
        inserted_conversations.id,
        inserted_conversations.shop_id,
        'SHOP'::message_sender_type,
        NULL::bigint,
        shop_account.user_id,
        'Chiều nay shop còn khung 16:30, bạn muốn đặt lịch không ạ?',
        now() - interval '15 minutes'
    FROM inserted_conversations
    JOIN seed_customers ON seed_customers.customer_id = inserted_conversations.customer_id
    CROSS JOIN shop_account
    WHERE seed_customers.phone = '0987654321'

    UNION ALL

    SELECT
        inserted_conversations.id,
        inserted_conversations.shop_id,
        'CUSTOMER'::message_sender_type,
        inserted_conversations.customer_id,
        NULL::bigint,
        'Mình muốn hỏi phí giao nhận thú cưng.',
        now() - interval '8 minutes'
    FROM inserted_conversations
    JOIN seed_customers ON seed_customers.customer_id = inserted_conversations.customer_id
    WHERE seed_customers.phone = '0901122334'
)
INSERT INTO messages (
    conversation_id,
    shop_id,
    sender_type,
    sender_customer_id,
    sender_user_id,
    body,
    created_at
)
SELECT
    seed_messages.conversation_id,
    seed_messages.shop_id,
    seed_messages.sender_type,
    seed_messages.sender_customer_id,
    seed_messages.sender_user_id,
    seed_messages.body,
    seed_messages.created_at
FROM seed_messages;

UPDATE conversations conversation
SET shop_last_read_message_id = read_state.last_read_message_id
FROM (
    SELECT
        conversations.id AS conversation_id,
        MAX(messages.id) FILTER (WHERE messages.sender_type = 'SHOP'::message_sender_type) AS last_read_message_id
    FROM conversations
    JOIN customers ON customers.id = conversations.customer_id
    JOIN messages ON messages.conversation_id = conversations.id
    WHERE conversations.shop_id = 1
      AND customers.phone IN ('096778899', '0987654321', '0901122334')
    GROUP BY conversations.id
) read_state
WHERE conversation.id = read_state.conversation_id;
