WITH seed_services AS (
    SELECT *
    FROM (
        VALUES
            (122::bigint, 86::bigint, '2026-07-12 17:00:00+07'::timestamptz),
            (120::bigint, 86::bigint, '2026-07-14 09:00:00+07'::timestamptz),
            (121::bigint, 85::bigint, '2026-07-14 10:30:00+07'::timestamptz),
            (122::bigint, 86::bigint, '2026-07-14 12:00:00+07'::timestamptz)
    ) AS seed(user_id, service_id, start_at)
),
bookings_to_seed AS (
    SELECT
        8::bigint AS shop_id,
        app_user.id AS user_id,
        customer.id AS customer_id,
        service.id AS service_id,
        service.name AS service_name,
        service.base_price AS unit_price,
        service.base_price AS total_amount,
        COALESCE(service.service_type, 'GENERAL') AS service_type,
        seed_services.start_at,
        seed_services.start_at + (COALESCE(NULLIF(service.duration_min, 0), 60) * INTERVAL '1 minute') AS end_at,
        'V115_FAKE_COMPLETED_BOOKING_S8_U' || app_user.id || '_S' || service.id || '_' || TO_CHAR(seed_services.start_at, 'YYYYMMDD') AS note
    FROM seed_services
    JOIN users app_user
      ON app_user.id = seed_services.user_id
    JOIN services service
      ON service.id = seed_services.service_id
     AND service.shop_id = 8
    LEFT JOIN LATERAL (
        SELECT c.id
        FROM customers c
        WHERE c.shop_id = 8
          AND c.user_id = app_user.id
        ORDER BY c.id
        LIMIT 1
    ) customer ON true
),
inserted_bookings AS (
    INSERT INTO bookings (
        shop_id,
        user_id,
        customer_id,
        start_at,
        end_at,
        status,
        source,
        note,
        created_by,
        created_at,
        updated_at
    )
    SELECT
        shop_id,
        user_id,
        customer_id,
        start_at,
        end_at,
        'COMPLETED'::booking_status,
        'CUSTOMER'::booking_source,
        note,
        NULL,
        start_at,
        start_at
    FROM bookings_to_seed
    WHERE NOT EXISTS (
        SELECT 1
        FROM bookings existing_booking
        WHERE existing_booking.shop_id = bookings_to_seed.shop_id
          AND existing_booking.note = bookings_to_seed.note
    )
    RETURNING id, shop_id, note
),
seeded_bookings AS (
    SELECT
        booking_row.id AS booking_id,
        seed_booking.shop_id,
        seed_booking.user_id,
        seed_booking.customer_id,
        seed_booking.service_id,
        seed_booking.service_name,
        seed_booking.unit_price,
        seed_booking.total_amount,
        seed_booking.service_type,
        seed_booking.start_at
    FROM bookings_to_seed seed_booking
    JOIN bookings booking_row
      ON booking_row.shop_id = seed_booking.shop_id
     AND booking_row.note = seed_booking.note
),
inserted_booking_items AS (
    INSERT INTO booking_items (shop_id, booking_id, item_type, ref_id, qty, unit_price, amount, created_at)
    SELECT
        shop_id,
        booking_id,
        'SERVICE'::booking_item_type,
        service_id,
        1,
        unit_price,
        total_amount,
        start_at
    FROM seeded_bookings
    WHERE NOT EXISTS (
        SELECT 1
        FROM booking_items existing_item
        WHERE existing_item.shop_id = seeded_bookings.shop_id
          AND existing_item.booking_id = seeded_bookings.booking_id
          AND existing_item.item_type = 'SERVICE'::booking_item_type
          AND existing_item.ref_id = seeded_bookings.service_id
    )
    RETURNING id
),
inserted_booking_invoices AS (
    INSERT INTO invoices (
        shop_id,
        user_id,
        customer_id,
        booking_id,
        total_amount,
        status,
        payment_method,
        issued_at,
        created_at,
        updated_at
    )
    SELECT
        shop_id,
        user_id,
        customer_id,
        booking_id,
        total_amount,
        'PAID'::invoice_status,
        'CASH',
        start_at,
        start_at,
        start_at
    FROM seeded_bookings
    WHERE NOT EXISTS (
        SELECT 1
        FROM invoices existing_invoice
        WHERE existing_invoice.shop_id = seeded_bookings.shop_id
          AND existing_invoice.booking_id = seeded_bookings.booking_id
    )
    RETURNING id, shop_id, booking_id
),
seeded_booking_invoices AS (
    SELECT
        invoice_row.id AS invoice_id,
        seeded_bookings.shop_id,
        seeded_bookings.booking_id,
        seeded_bookings.service_id,
        seeded_bookings.service_name,
        seeded_bookings.unit_price,
        seeded_bookings.total_amount
    FROM seeded_bookings
    JOIN (
        SELECT id, shop_id, booking_id
        FROM inserted_booking_invoices

        UNION ALL

        SELECT existing_invoice.id, existing_invoice.shop_id, existing_invoice.booking_id
        FROM invoices existing_invoice
        JOIN seeded_bookings seeded_booking
          ON seeded_booking.shop_id = existing_invoice.shop_id
         AND seeded_booking.booking_id = existing_invoice.booking_id
        WHERE NOT EXISTS (
            SELECT 1
            FROM inserted_booking_invoices inserted_invoice
            WHERE inserted_invoice.shop_id = existing_invoice.shop_id
              AND inserted_invoice.booking_id = existing_invoice.booking_id
        )
    ) invoice_row
      ON invoice_row.shop_id = seeded_bookings.shop_id
     AND invoice_row.booking_id = seeded_bookings.booking_id
),
inserted_booking_invoice_lines AS (
    INSERT INTO invoice_lines (shop_id, invoice_id, line_type, ref_id, item_name, qty, unit_price, amount)
    SELECT
        shop_id,
        invoice_id,
        'SERVICE',
        service_id,
        service_name,
        1,
        unit_price,
        total_amount
    FROM seeded_booking_invoices
    WHERE NOT EXISTS (
        SELECT 1
        FROM invoice_lines existing_line
        WHERE existing_line.shop_id = seeded_booking_invoices.shop_id
          AND existing_line.invoice_id = seeded_booking_invoices.invoice_id
          AND existing_line.line_type = 'SERVICE'
          AND existing_line.ref_id = seeded_booking_invoices.service_id
    )
    RETURNING id
)
INSERT INTO platform_commissions (
    shop_id,
    source_type,
    source_id,
    gross_amount,
    discount_amount,
    shipping_fee,
    commission_base,
    commission_rate_bps,
    commission_amount,
    status,
    created_at
)
SELECT
    shop_id,
    CASE
        WHEN service_type = 'VETERINARY' THEN 'VET_BOOKING'::commission_source_type
        ELSE 'SERVICE_BOOKING'::commission_source_type
    END,
    booking_id,
    total_amount,
    0,
    0,
    total_amount,
    1500,
    ROUND(total_amount * 0.15)::bigint,
    'PENDING'::commission_status,
    start_at
FROM seeded_bookings
WHERE NOT EXISTS (
    SELECT 1
    FROM platform_commissions existing_commission
    WHERE existing_commission.source_type = CASE
            WHEN seeded_bookings.service_type = 'VETERINARY' THEN 'VET_BOOKING'::commission_source_type
            ELSE 'SERVICE_BOOKING'::commission_source_type
        END
      AND existing_commission.source_id = seeded_bookings.booking_id
);
