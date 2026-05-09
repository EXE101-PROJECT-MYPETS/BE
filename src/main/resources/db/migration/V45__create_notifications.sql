CREATE TABLE IF NOT EXISTS notifications (
    id bigserial PRIMARY KEY,
    type varchar(64) NOT NULL,
    recipient_type varchar(32) NOT NULL,
    recipient_user_id bigint REFERENCES users(id) ON DELETE CASCADE,
    recipient_shop_id bigint REFERENCES shops(id) ON DELETE CASCADE,
    shop_id bigint REFERENCES shops(id) ON DELETE CASCADE,
    target_type varchar(64) NOT NULL,
    target_id bigint,
    actor_user_id bigint REFERENCES users(id) ON DELETE SET NULL,
    title varchar(255) NOT NULL,
    body text,
    metadata_json text,
    read_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    CHECK (
        (recipient_type = 'USER' AND recipient_user_id IS NOT NULL AND recipient_shop_id IS NULL)
        OR
        (recipient_type = 'SHOP' AND recipient_shop_id IS NOT NULL AND recipient_user_id IS NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_id_desc
    ON notifications(recipient_user_id, id DESC)
    WHERE recipient_type = 'USER';

CREATE INDEX IF NOT EXISTS idx_notifications_shop_id_desc
    ON notifications(recipient_shop_id, id DESC)
    WHERE recipient_type = 'SHOP';

CREATE INDEX IF NOT EXISTS idx_notifications_user_unread
    ON notifications(recipient_user_id, read_at)
    WHERE recipient_type = 'USER' AND read_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_notifications_shop_unread
    ON notifications(recipient_shop_id, read_at)
    WHERE recipient_type = 'SHOP' AND read_at IS NULL;
