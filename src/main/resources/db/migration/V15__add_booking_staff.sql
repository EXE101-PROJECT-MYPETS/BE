CREATE TABLE IF NOT EXISTS booking_staff (
    shop_id bigint NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    booking_id bigint NOT NULL,
    user_id bigint NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),

    PRIMARY KEY (booking_id, user_id),

    CONSTRAINT fk_booking_staff_booking_shop
        FOREIGN KEY (shop_id, booking_id)
        REFERENCES bookings(shop_id, id)
        ON DELETE CASCADE,

    CONSTRAINT fk_booking_staff_member_shop
        FOREIGN KEY (shop_id, user_id)
        REFERENCES shop_members(shop_id, user_id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_booking_staff_shop_booking
    ON booking_staff(shop_id, booking_id);

CREATE INDEX IF NOT EXISTS idx_booking_staff_user_id
    ON booking_staff(user_id);
