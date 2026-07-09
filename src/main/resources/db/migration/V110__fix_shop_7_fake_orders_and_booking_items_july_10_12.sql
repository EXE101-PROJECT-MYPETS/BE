CREATE TEMP TABLE tmp_v110_wrong_shop_7_orders ON COMMIT DROP AS
SELECT id, shop_id
FROM orders
WHERE shop_id = 7
  AND order_code IN (
      'FAKE-S7-U68-P41-20260710',
      'FAKE-S7-U68-P42-20260710',
      'FAKE-S7-U68-P43-20260710',
      'FAKE-S7-U69-P41-20260711',
      'FAKE-S7-U69-P42-20260711',
      'FAKE-S7-U69-P43-20260711',
      'FAKE-S7-U70-P41-20260712',
      'FAKE-S7-U70-P42-20260712',
      'FAKE-S7-U70-P43-20260712'
  );

DELETE FROM platform_commission_invoice_items invoice_item
WHERE EXISTS (
    SELECT 1
    FROM platform_commissions commission
    JOIN tmp_v110_wrong_shop_7_orders wrong_order
      ON wrong_order.id = commission.source_id
    WHERE commission.source_type = 'ORDER'::commission_source_type
      AND invoice_item.commission_id = commission.id
);

DELETE FROM platform_commissions commission
WHERE commission.source_type = 'ORDER'::commission_source_type
  AND EXISTS (
      SELECT 1
      FROM tmp_v110_wrong_shop_7_orders wrong_order
      WHERE wrong_order.id = commission.source_id
  );

DELETE FROM invoice_lines invoice_line
WHERE EXISTS (
    SELECT 1
    FROM invoices invoice
    JOIN tmp_v110_wrong_shop_7_orders wrong_order
      ON wrong_order.shop_id = invoice.shop_id
     AND wrong_order.id = invoice.order_id
    WHERE invoice_line.shop_id = invoice.shop_id
      AND invoice_line.invoice_id = invoice.id
);

DELETE FROM invoices invoice
WHERE EXISTS (
    SELECT 1
    FROM tmp_v110_wrong_shop_7_orders wrong_order
    WHERE wrong_order.shop_id = invoice.shop_id
      AND wrong_order.id = invoice.order_id
);

DELETE FROM order_items order_item
WHERE EXISTS (
    SELECT 1
    FROM tmp_v110_wrong_shop_7_orders wrong_order
    WHERE wrong_order.shop_id = order_item.shop_id
      AND wrong_order.id = order_item.order_id
);

DELETE FROM orders order_row
WHERE EXISTS (
    SELECT 1
    FROM tmp_v110_wrong_shop_7_orders wrong_order
    WHERE wrong_order.shop_id = order_row.shop_id
      AND wrong_order.id = order_row.id
);

WITH seed_products AS (
    SELECT *
    FROM (
        VALUES
            (68::bigint, 45::bigint, '2026-07-10 09:15:00+07'::timestamptz),
            (69::bigint, 45::bigint, '2026-07-11 09:15:00+07'::timestamptz),
            (70::bigint, 45::bigint, '2026-07-12 09:15:00+07'::timestamptz)
    ) AS seed(user_id, product_id, completed_at)
),
seed_order_context AS (
    SELECT
        7::bigint AS shop_id,
        app_user.id AS user_id,
        customer.id AS customer_id,
        COALESCE(NULLIF(btrim(address.name), ''), NULLIF(btrim(app_user.full_name), ''), 'Khach hang ' || app_user.id) AS receiver_name,
        COALESCE(NULLIF(btrim(address.tel), ''), NULLIF(btrim(app_user.phone), ''), '09000000' || app_user.id) AS receiver_phone,
        COALESCE(NULLIF(btrim(address.address), ''), NULLIF(btrim(app_user.address), ''), 'Dia chi demo') AS shipping_address,
        COALESCE(NULLIF(btrim(address.province), ''), 'TP. Ho Chi Minh') AS shipping_province,
        COALESCE(NULLIF(btrim(address.district), ''), 'Quan 1') AS shipping_district,
        COALESCE(NULLIF(btrim(address.ward), ''), 'Phuong Ben Nghe') AS shipping_ward,
        COALESCE(NULLIF(btrim(address.hamlet), ''), 'Khac') AS shipping_hamlet,
        address.id AS user_address_id
    FROM users app_user
    LEFT JOIN LATERAL (
        SELECT c.id
        FROM customers c
        WHERE c.shop_id = 7
          AND c.user_id = app_user.id
        ORDER BY c.id
        LIMIT 1
    ) customer ON true
    LEFT JOIN LATERAL (
        SELECT ua.*
        FROM user_addresses ua
        WHERE ua.user_id = app_user.id
        ORDER BY ua.is_default DESC, ua.id
        LIMIT 1
    ) address ON true
    WHERE app_user.id IN (68, 69, 70)
),
orders_to_seed AS (
    SELECT
        context.shop_id,
        context.user_id,
        context.customer_id,
        context.user_address_id,
        product.id AS product_id,
        product.name AS product_name,
        product.price AS unit_price,
        product.price AS total_amount,
        seed_products.completed_at,
        'FAKE-S7-U' || context.user_id || '-P' || product.id || '-' || TO_CHAR(seed_products.completed_at, 'YYYYMMDD') AS order_code,
        context.receiver_name,
        context.receiver_phone,
        context.shipping_address,
        context.shipping_province,
        context.shipping_district,
        context.shipping_ward,
        context.shipping_hamlet
    FROM seed_products
    JOIN products product
      ON product.id = seed_products.product_id
     AND product.shop_id = 7
    JOIN seed_order_context context
      ON context.user_id = seed_products.user_id
),
inserted_orders AS (
    INSERT INTO orders (
        shop_id,
        user_id,
        customer_id,
        user_address_id,
        order_code,
        status,
        source,
        subtotal_amount,
        shipping_fee,
        discount_amount,
        total_amount,
        receiver_name,
        receiver_phone,
        shipping_address,
        shipping_province,
        shipping_district,
        shipping_ward,
        shipping_hamlet,
        note,
        created_at,
        updated_at
    )
    SELECT
        shop_id,
        user_id,
        customer_id,
        user_address_id,
        order_code,
        'COMPLETED'::order_status,
        'ONLINE'::order_source,
        total_amount,
        0,
        0,
        total_amount,
        receiver_name,
        receiver_phone,
        shipping_address,
        shipping_province,
        shipping_district,
        shipping_ward,
        shipping_hamlet,
        'V110_FAKE_COMPLETED_PRODUCT_ORDER',
        completed_at,
        completed_at
    FROM orders_to_seed
    WHERE NOT EXISTS (
        SELECT 1
        FROM orders existing_order
        WHERE existing_order.shop_id = orders_to_seed.shop_id
          AND existing_order.order_code = orders_to_seed.order_code
    )
    RETURNING id, shop_id, order_code
),
seeded_orders AS (
    SELECT
        order_row.id AS order_id,
        seed_order.shop_id,
        seed_order.user_id,
        seed_order.customer_id,
        seed_order.product_id,
        seed_order.product_name,
        seed_order.unit_price,
        seed_order.total_amount,
        seed_order.completed_at
    FROM orders_to_seed seed_order
    JOIN orders order_row
      ON order_row.shop_id = seed_order.shop_id
     AND order_row.order_code = seed_order.order_code
),
inserted_order_items AS (
    INSERT INTO order_items (shop_id, order_id, product_id, qty, unit_price, amount, created_at)
    SELECT shop_id, order_id, product_id, 1, unit_price, total_amount, completed_at
    FROM seeded_orders
    WHERE NOT EXISTS (
        SELECT 1
        FROM order_items existing_item
        WHERE existing_item.shop_id = seeded_orders.shop_id
          AND existing_item.order_id = seeded_orders.order_id
          AND existing_item.product_id = seeded_orders.product_id
    )
    RETURNING id
),
inserted_order_invoices AS (
    INSERT INTO invoices (
        shop_id,
        user_id,
        customer_id,
        order_id,
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
        order_id,
        total_amount,
        'PAID'::invoice_status,
        'CASH',
        completed_at,
        completed_at,
        completed_at
    FROM seeded_orders
    WHERE NOT EXISTS (
        SELECT 1
        FROM invoices existing_invoice
        WHERE existing_invoice.shop_id = seeded_orders.shop_id
          AND existing_invoice.order_id = seeded_orders.order_id
    )
    RETURNING id, shop_id, order_id
),
seeded_order_invoices AS (
    SELECT
        invoice_row.id AS invoice_id,
        seeded_orders.shop_id,
        seeded_orders.product_id,
        seeded_orders.product_name,
        seeded_orders.unit_price,
        seeded_orders.total_amount
    FROM seeded_orders
    JOIN (
        SELECT id, shop_id, order_id
        FROM inserted_order_invoices

        UNION ALL

        SELECT existing_invoice.id, existing_invoice.shop_id, existing_invoice.order_id
        FROM invoices existing_invoice
        JOIN seeded_orders seeded_order
          ON seeded_order.shop_id = existing_invoice.shop_id
         AND seeded_order.order_id = existing_invoice.order_id
        WHERE NOT EXISTS (
            SELECT 1
            FROM inserted_order_invoices inserted_invoice
            WHERE inserted_invoice.shop_id = existing_invoice.shop_id
              AND inserted_invoice.order_id = existing_invoice.order_id
        )
    ) invoice_row
      ON invoice_row.shop_id = seeded_orders.shop_id
     AND invoice_row.order_id = seeded_orders.order_id
),
inserted_order_invoice_lines AS (
    INSERT INTO invoice_lines (shop_id, invoice_id, line_type, ref_id, item_name, qty, unit_price, amount)
    SELECT shop_id, invoice_id, 'PRODUCT', product_id, product_name, 1, unit_price, total_amount
    FROM seeded_order_invoices
    WHERE NOT EXISTS (
        SELECT 1
        FROM invoice_lines existing_line
        WHERE existing_line.shop_id = seeded_order_invoices.shop_id
          AND existing_line.invoice_id = seeded_order_invoices.invoice_id
          AND existing_line.line_type = 'PRODUCT'
          AND existing_line.ref_id = seeded_order_invoices.product_id
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
    'ORDER'::commission_source_type,
    order_id,
    total_amount,
    0,
    0,
    total_amount,
    1500,
    ROUND(total_amount * 0.15)::bigint,
    'PENDING'::commission_status,
    completed_at
FROM seeded_orders
WHERE NOT EXISTS (
    SELECT 1
    FROM platform_commissions existing_commission
    WHERE existing_commission.source_type = 'ORDER'::commission_source_type
      AND existing_commission.source_id = seeded_orders.order_id
);

WITH seed_services AS (
    SELECT *
    FROM (
        VALUES
            (68::bigint, 78::bigint, '2026-07-10 10:00:00+07'::timestamptz),
            (68::bigint, 79::bigint, '2026-07-10 14:00:00+07'::timestamptz),
            (68::bigint, 80::bigint, '2026-07-10 16:30:00+07'::timestamptz),
            (69::bigint, 78::bigint, '2026-07-11 10:00:00+07'::timestamptz),
            (69::bigint, 79::bigint, '2026-07-11 14:00:00+07'::timestamptz),
            (69::bigint, 80::bigint, '2026-07-11 16:30:00+07'::timestamptz),
            (70::bigint, 78::bigint, '2026-07-12 10:00:00+07'::timestamptz),
            (70::bigint, 79::bigint, '2026-07-12 14:00:00+07'::timestamptz),
            (70::bigint, 80::bigint, '2026-07-12 16:30:00+07'::timestamptz)
    ) AS seed(user_id, service_id, start_at)
),
bookings_to_seed AS (
    SELECT
        7::bigint AS shop_id,
        app_user.id AS user_id,
        customer.id AS customer_id,
        service.id AS service_id,
        service.name AS service_name,
        service.base_price AS unit_price,
        service.base_price AS total_amount,
        COALESCE(service.service_type, 'GENERAL') AS service_type,
        seed_services.start_at,
        seed_services.start_at + (COALESCE(NULLIF(service.duration_min, 0), 60) * INTERVAL '1 minute') AS end_at,
        'V108_FAKE_COMPLETED_BOOKING_S7_U' || app_user.id || '_S' || service.id || '_' || TO_CHAR(seed_services.start_at, 'YYYYMMDD') AS note
    FROM seed_services
    JOIN users app_user
      ON app_user.id = seed_services.user_id
    JOIN services service
      ON service.id = seed_services.service_id
     AND service.shop_id = 7
    LEFT JOIN LATERAL (
        SELECT c.id
        FROM customers c
        WHERE c.shop_id = 7
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
updated_booking_invoices AS (
    UPDATE invoices invoice
    SET total_amount = seeded_bookings.total_amount,
        status = 'PAID'::invoice_status,
        payment_method = 'CASH',
        issued_at = seeded_bookings.start_at,
        updated_at = seeded_bookings.start_at
    FROM seeded_bookings
    WHERE invoice.shop_id = seeded_bookings.shop_id
      AND invoice.booking_id = seeded_bookings.booking_id
    RETURNING invoice.id
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
