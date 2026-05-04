WITH seed_bookings AS (
    SELECT
        1 AS shop_id,
        booking_days.booking_date::date AS booking_date,
        booking_slots.slot_no,
        booking_slots.customer_phone,
        booking_slots.start_time,
        booking_slots.end_time,
        booking_slots.service_name,
        CONCAT(
                'V24_CONFIRMED_BOOKING_',
                TO_CHAR(booking_days.booking_date, 'YYYYMMDD'),
                '_',
                booking_slots.slot_no
        ) AS note
    FROM GENERATE_SERIES(
            '2026-04-28'::date,
            '2026-05-15'::date,
            '1 day'::interval
    ) AS booking_days(booking_date)
    CROSS JOIN (
        VALUES
            (1, '096778899', '09:00'::time, '10:00'::time, 'Tam co ban'),
            (2, '0912345678', '10:30'::time, '11:30'::time, 'Tam duong long'),
            (3, '0987654321', '13:30'::time, '14:30'::time, 'Grooming toan dien'),
            (4, '0944556677', '15:00'::time, '16:00'::time, 'Cat tia nhanh'),
            (5, '0901122334', '16:30'::time, '17:30'::time, 'Tam cham soc da')
    ) AS booking_slots(slot_no, customer_phone, start_time, end_time, service_name)
)
INSERT INTO bookings (shop_id, customer_id, start_at, end_at, status, source, note, created_by)
SELECT
    seed_bookings.shop_id,
    customers.id,
    (seed_bookings.booking_date + seed_bookings.start_time) AT TIME ZONE 'Asia/Ho_Chi_Minh',
    (seed_bookings.booking_date + seed_bookings.end_time) AT TIME ZONE 'Asia/Ho_Chi_Minh',
    'CONFIRMED'::booking_status,
    'STAFF'::booking_source,
    seed_bookings.note,
    NULL
FROM seed_bookings
JOIN customers
    ON customers.shop_id = seed_bookings.shop_id
    AND customers.phone = seed_bookings.customer_phone
WHERE EXISTS (
    SELECT 1
    FROM shops
    WHERE shops.id = seed_bookings.shop_id
)
AND NOT EXISTS (
    SELECT 1
    FROM bookings
    WHERE bookings.shop_id = seed_bookings.shop_id
      AND bookings.note = seed_bookings.note
);

WITH seed_bookings AS (
    SELECT
        1 AS shop_id,
        booking_days.booking_date::date AS booking_date,
        booking_slots.slot_no,
        booking_slots.service_name,
        CONCAT(
                'V24_CONFIRMED_BOOKING_',
                TO_CHAR(booking_days.booking_date, 'YYYYMMDD'),
                '_',
                booking_slots.slot_no
        ) AS note
    FROM GENERATE_SERIES(
            '2026-04-28'::date,
            '2026-05-15'::date,
            '1 day'::interval
    ) AS booking_days(booking_date)
    CROSS JOIN (
        VALUES
            (1, 'Tam co ban'),
            (2, 'Tam duong long'),
            (3, 'Grooming toan dien'),
            (4, 'Cat tia nhanh'),
            (5, 'Tam cham soc da')
    ) AS booking_slots(slot_no, service_name)
)
INSERT INTO booking_items (shop_id, booking_id, item_type, ref_id, qty, unit_price, amount)
SELECT
    bookings.shop_id,
    bookings.id,
    'SERVICE'::booking_item_type,
    services.id,
    1,
    services.base_price,
    services.base_price
FROM seed_bookings
JOIN bookings
    ON bookings.shop_id = seed_bookings.shop_id
    AND bookings.note = seed_bookings.note
JOIN services
    ON services.shop_id = bookings.shop_id
    AND services.name = seed_bookings.service_name
WHERE NOT EXISTS (
    SELECT 1
    FROM booking_items
    WHERE booking_items.booking_id = bookings.id
      AND booking_items.item_type = 'SERVICE'::booking_item_type
      AND booking_items.ref_id = services.id
);
