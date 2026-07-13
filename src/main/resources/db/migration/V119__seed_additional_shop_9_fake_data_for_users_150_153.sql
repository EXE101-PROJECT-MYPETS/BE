DROP TABLE IF EXISTS seed_shop_9_additional_users;

CREATE TEMP TABLE seed_shop_9_additional_users ON COMMIT DROP AS
SELECT
    seed_user.seed_slot,
    app_user.id,
    app_user.full_name,
    app_user.phone,
    app_user.email,
    app_user.address
FROM (
    VALUES
        (1::bigint, 150::bigint),
        (2::bigint, 151::bigint),
        (3::bigint, 152::bigint),
        (4::bigint, 153::bigint)
) AS seed_user(seed_slot, user_id)
JOIN users app_user
  ON app_user.id = seed_user.user_id
WHERE app_user.role = 'CUSTOMER'::user_role
  AND app_user.status = 'ACTIVE'::user_status
  AND EXISTS (
      SELECT 1
      FROM shops shop
      WHERE shop.id = 9
  );

DROP TABLE IF EXISTS seed_shop_9_additional_products;

CREATE TEMP TABLE seed_shop_9_additional_products ON COMMIT DROP AS
SELECT
    ROW_NUMBER() OVER (ORDER BY product.active DESC, product.id) AS seed_slot,
    product.id AS product_id,
    product.name AS product_name,
    product.price AS unit_price
FROM products product
WHERE product.shop_id = 9
ORDER BY product.active DESC, product.id
LIMIT 4;

DROP TABLE IF EXISTS seed_shop_9_additional_services;

CREATE TEMP TABLE seed_shop_9_additional_services ON COMMIT DROP AS
SELECT
    ROW_NUMBER() OVER (ORDER BY service.active DESC, service.id) AS seed_slot,
    service.id AS service_id,
    service.name AS service_name,
    service.base_price AS unit_price,
    COALESCE(NULLIF(service.duration_min, 0), 60) AS duration_min,
    COALESCE(service.service_type, 'GENERAL') AS service_type
FROM services service
WHERE service.shop_id = 9
ORDER BY service.active DESC, service.id
LIMIT 4;

UPDATE customers customer
SET user_id = target_user.id,
    full_name = COALESCE(NULLIF(btrim(target_user.full_name), ''), customer.full_name),
    phone = COALESCE(NULLIF(btrim(customer.phone), ''), NULLIF(btrim(target_user.phone), '')),
    updated_at = now()
FROM seed_shop_9_additional_users target_user
WHERE customer.shop_id = 9
  AND customer.email IS NOT NULL
  AND target_user.email IS NOT NULL
  AND customer.email = target_user.email
  AND (customer.user_id IS NULL OR customer.user_id = target_user.id);

INSERT INTO customers (shop_id, user_id, full_name, phone, email, created_at, updated_at)
SELECT
    9,
    target_user.id,
    COALESCE(NULLIF(btrim(target_user.full_name), ''), 'Khach hang ' || target_user.id),
    CASE
        WHEN NULLIF(btrim(target_user.phone), '') IS NULL THEN NULL
        WHEN EXISTS (
            SELECT 1
            FROM customers existing_customer
            WHERE existing_customer.shop_id = 9
              AND existing_customer.phone = NULLIF(btrim(target_user.phone), '')
        ) THEN NULL
        ELSE NULLIF(btrim(target_user.phone), '')
    END,
    NULLIF(btrim(target_user.email), ''),
    now(),
    now()
FROM seed_shop_9_additional_users target_user
WHERE NOT EXISTS (
    SELECT 1
    FROM customers customer
    WHERE customer.shop_id = 9
      AND customer.user_id = target_user.id
)
  AND NOT EXISTS (
      SELECT 1
      FROM customers customer
      WHERE customer.shop_id = 9
        AND target_user.email IS NOT NULL
        AND customer.email = target_user.email
  );

WITH seed_product_orders AS (
    SELECT *
    FROM (
        VALUES
            (1::bigint, 1::bigint, '2026-07-15 09:00:00+07'::timestamptz),
            (2::bigint, 2::bigint, '2026-07-15 09:30:00+07'::timestamptz),
            (3::bigint, 3::bigint, '2026-07-15 10:00:00+07'::timestamptz),
            (4::bigint, 4::bigint, '2026-07-15 10:30:00+07'::timestamptz)
    ) AS seed(user_slot, product_slot, completed_at)
),
orders_to_seed AS (
    SELECT
        9::bigint AS shop_id,
        target_user.id AS user_id,
        customer.id AS customer_id,
        address.id AS user_address_id,
        product.product_id,
        product.product_name,
        product.unit_price,
        product.unit_price AS total_amount,
        seed_product_orders.completed_at,
        'FAKE-S9-U' || target_user.id || '-P' || product.product_id || '-' || TO_CHAR(seed_product_orders.completed_at, 'YYYYMMDD') AS order_code,
        COALESCE(NULLIF(btrim(address.name), ''), NULLIF(btrim(target_user.full_name), ''), 'Khach hang ' || target_user.id) AS receiver_name,
        COALESCE(NULLIF(btrim(address.tel), ''), NULLIF(btrim(target_user.phone), ''), '0900000' || target_user.id) AS receiver_phone,
        COALESCE(NULLIF(btrim(address.address), ''), NULLIF(btrim(target_user.address), ''), 'Dia chi demo') AS shipping_address,
        COALESCE(NULLIF(btrim(address.province), ''), 'TP. Ho Chi Minh') AS shipping_province,
        COALESCE(NULLIF(btrim(address.district), ''), 'Quan 1') AS shipping_district,
        COALESCE(NULLIF(btrim(address.ward), ''), 'Phuong Ben Nghe') AS shipping_ward,
        COALESCE(NULLIF(btrim(address.hamlet), ''), 'Khac') AS shipping_hamlet
    FROM seed_product_orders
    JOIN seed_shop_9_additional_users target_user
      ON target_user.seed_slot = seed_product_orders.user_slot
    JOIN seed_shop_9_additional_products product
      ON product.seed_slot = seed_product_orders.product_slot
    LEFT JOIN LATERAL (
        SELECT c.id
        FROM customers c
        WHERE c.shop_id = 9
          AND (
              c.user_id = target_user.id
              OR (
                  target_user.email IS NOT NULL
                  AND c.email = target_user.email
              )
          )
        ORDER BY
            CASE WHEN c.user_id = target_user.id THEN 0 ELSE 1 END,
            c.id
        LIMIT 1
    ) customer ON true
    LEFT JOIN LATERAL (
        SELECT ua.*
        FROM user_addresses ua
        WHERE ua.user_id = target_user.id
        ORDER BY ua.is_default DESC, ua.id
        LIMIT 1
    ) address ON true
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
        'V119_FAKE_COMPLETED_PRODUCT_ORDER',
        completed_at,
        completed_at
    FROM orders_to_seed
    WHERE customer_id IS NOT NULL
      AND NOT EXISTS (
          SELECT 1
          FROM orders existing_order
          WHERE existing_order.shop_id = orders_to_seed.shop_id
            AND existing_order.order_code = orders_to_seed.order_code
      )
    RETURNING id, shop_id, order_code
),
order_rows AS (
    SELECT id, shop_id, order_code
    FROM inserted_orders

    UNION ALL

    SELECT existing_order.id, existing_order.shop_id, existing_order.order_code
    FROM orders existing_order
    JOIN orders_to_seed seed_order
      ON seed_order.shop_id = existing_order.shop_id
     AND seed_order.order_code = existing_order.order_code
    WHERE NOT EXISTS (
        SELECT 1
        FROM inserted_orders inserted_order
        WHERE inserted_order.shop_id = existing_order.shop_id
          AND inserted_order.order_code = existing_order.order_code
    )
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
    JOIN order_rows order_row
      ON order_row.shop_id = seed_order.shop_id
     AND order_row.order_code = seed_order.order_code
),
updated_orders AS (
    UPDATE orders order_row
    SET user_id = seeded_order.user_id,
        customer_id = seeded_order.customer_id,
        subtotal_amount = seeded_order.total_amount,
        shipping_fee = 0,
        discount_amount = 0,
        total_amount = seeded_order.total_amount,
        status = 'COMPLETED'::order_status,
        updated_at = seeded_order.completed_at
    FROM seeded_orders seeded_order
    WHERE order_row.shop_id = seeded_order.shop_id
      AND order_row.id = seeded_order.order_id
    RETURNING order_row.id
),
updated_order_items AS (
    UPDATE order_items order_item
    SET qty = 1,
        unit_price = seeded_order.unit_price,
        amount = seeded_order.total_amount
    FROM seeded_orders seeded_order
    WHERE order_item.shop_id = seeded_order.shop_id
      AND order_item.order_id = seeded_order.order_id
      AND order_item.product_id = seeded_order.product_id
    RETURNING order_item.id
),
inserted_order_items AS (
    INSERT INTO order_items (shop_id, order_id, product_id, qty, unit_price, amount, created_at)
    SELECT
        shop_id,
        order_id,
        product_id,
        1,
        unit_price,
        total_amount,
        completed_at
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
updated_order_invoices AS (
    UPDATE invoices invoice
    SET user_id = seeded_order.user_id,
        customer_id = seeded_order.customer_id,
        total_amount = seeded_order.total_amount,
        status = 'PAID'::invoice_status,
        payment_method = 'CASH',
        issued_at = seeded_order.completed_at,
        updated_at = seeded_order.completed_at
    FROM seeded_orders seeded_order
    WHERE invoice.shop_id = seeded_order.shop_id
      AND invoice.order_id = seeded_order.order_id
    RETURNING invoice.id, invoice.shop_id, invoice.order_id
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
        seeded_orders.order_id,
        seeded_orders.product_id,
        seeded_orders.product_name,
        seeded_orders.unit_price,
        seeded_orders.total_amount
    FROM seeded_orders
    JOIN (
        SELECT id, shop_id, order_id
        FROM updated_order_invoices

        UNION ALL

        SELECT id, shop_id, order_id
        FROM inserted_order_invoices
    ) invoice_row
      ON invoice_row.shop_id = seeded_orders.shop_id
     AND invoice_row.order_id = seeded_orders.order_id
),
updated_order_invoice_lines AS (
    UPDATE invoice_lines invoice_line
    SET item_name = seeded_order_invoice.product_name,
        qty = 1,
        unit_price = seeded_order_invoice.unit_price,
        amount = seeded_order_invoice.total_amount
    FROM seeded_order_invoices seeded_order_invoice
    WHERE invoice_line.shop_id = seeded_order_invoice.shop_id
      AND invoice_line.invoice_id = seeded_order_invoice.invoice_id
      AND invoice_line.line_type = 'PRODUCT'
      AND invoice_line.ref_id = seeded_order_invoice.product_id
    RETURNING invoice_line.id
),
inserted_order_invoice_lines AS (
    INSERT INTO invoice_lines (shop_id, invoice_id, line_type, ref_id, item_name, qty, unit_price, amount)
    SELECT
        shop_id,
        invoice_id,
        'PRODUCT',
        product_id,
        product_name,
        1,
        unit_price,
        total_amount
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
),
updated_order_commissions AS (
    UPDATE platform_commissions commission
    SET gross_amount = seeded_order.total_amount,
        discount_amount = 0,
        shipping_fee = 0,
        commission_base = seeded_order.total_amount,
        commission_rate_bps = 1500,
        commission_amount = ROUND(seeded_order.total_amount * 0.15)::bigint,
        status = 'PENDING'::commission_status
    FROM seeded_orders seeded_order
    WHERE commission.source_type = 'ORDER'::commission_source_type
      AND commission.source_id = seeded_order.order_id
    RETURNING commission.id
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
            (1::bigint, 1::bigint, '2026-07-15 14:00:00+07'::timestamptz),
            (2::bigint, 2::bigint, '2026-07-15 15:30:00+07'::timestamptz),
            (3::bigint, 3::bigint, '2026-07-15 17:00:00+07'::timestamptz),
            (4::bigint, 4::bigint, '2026-07-16 09:00:00+07'::timestamptz)
    ) AS seed(user_slot, service_slot, start_at)
),
bookings_to_seed AS (
    SELECT
        9::bigint AS shop_id,
        target_user.id AS user_id,
        customer.id AS customer_id,
        service.service_id,
        service.service_name,
        service.unit_price,
        service.unit_price AS total_amount,
        service.service_type,
        seed_services.start_at,
        seed_services.start_at + (service.duration_min * INTERVAL '1 minute') AS end_at,
        'V119_FAKE_COMPLETED_BOOKING_S9_U' || target_user.id || '_S' || service.service_id || '_' || TO_CHAR(seed_services.start_at, 'YYYYMMDD') AS note
    FROM seed_services
    JOIN seed_shop_9_additional_users target_user
      ON target_user.seed_slot = seed_services.user_slot
    JOIN seed_shop_9_additional_services service
      ON service.seed_slot = seed_services.service_slot
    LEFT JOIN LATERAL (
        SELECT c.id
        FROM customers c
        WHERE c.shop_id = 9
          AND (
              c.user_id = target_user.id
              OR (
                  target_user.email IS NOT NULL
                  AND c.email = target_user.email
              )
          )
        ORDER BY
            CASE WHEN c.user_id = target_user.id THEN 0 ELSE 1 END,
            c.id
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
    WHERE customer_id IS NOT NULL
      AND NOT EXISTS (
          SELECT 1
          FROM bookings existing_booking
          WHERE existing_booking.shop_id = bookings_to_seed.shop_id
            AND existing_booking.note = bookings_to_seed.note
      )
    RETURNING id, shop_id, note
),
booking_rows AS (
    SELECT id, shop_id, note
    FROM inserted_bookings

    UNION ALL

    SELECT existing_booking.id, existing_booking.shop_id, existing_booking.note
    FROM bookings existing_booking
    JOIN bookings_to_seed seed_booking
      ON seed_booking.shop_id = existing_booking.shop_id
     AND seed_booking.note = existing_booking.note
    WHERE NOT EXISTS (
        SELECT 1
        FROM inserted_bookings inserted_booking
        WHERE inserted_booking.shop_id = existing_booking.shop_id
          AND inserted_booking.note = existing_booking.note
    )
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
    JOIN booking_rows booking_row
      ON booking_row.shop_id = seed_booking.shop_id
     AND booking_row.note = seed_booking.note
),
updated_bookings AS (
    UPDATE bookings booking
    SET user_id = seeded_booking.user_id,
        customer_id = seeded_booking.customer_id,
        status = 'COMPLETED'::booking_status,
        updated_at = seeded_booking.start_at
    FROM seeded_bookings seeded_booking
    WHERE booking.shop_id = seeded_booking.shop_id
      AND booking.id = seeded_booking.booking_id
    RETURNING booking.id
),
updated_booking_items AS (
    UPDATE booking_items booking_item
    SET qty = 1,
        unit_price = seeded_booking.unit_price,
        amount = seeded_booking.total_amount
    FROM seeded_bookings seeded_booking
    WHERE booking_item.shop_id = seeded_booking.shop_id
      AND booking_item.booking_id = seeded_booking.booking_id
      AND booking_item.item_type = 'SERVICE'::booking_item_type
      AND booking_item.ref_id = seeded_booking.service_id
    RETURNING booking_item.id
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
    SET user_id = seeded_booking.user_id,
        customer_id = seeded_booking.customer_id,
        total_amount = seeded_booking.total_amount,
        status = 'PAID'::invoice_status,
        payment_method = 'CASH',
        issued_at = seeded_booking.start_at,
        updated_at = seeded_booking.start_at
    FROM seeded_bookings seeded_booking
    WHERE invoice.shop_id = seeded_booking.shop_id
      AND invoice.booking_id = seeded_booking.booking_id
    RETURNING invoice.id, invoice.shop_id, invoice.booking_id
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
        FROM updated_booking_invoices

        UNION ALL

        SELECT id, shop_id, booking_id
        FROM inserted_booking_invoices
    ) invoice_row
      ON invoice_row.shop_id = seeded_bookings.shop_id
     AND invoice_row.booking_id = seeded_bookings.booking_id
),
updated_booking_invoice_lines AS (
    UPDATE invoice_lines invoice_line
    SET item_name = seeded_booking_invoice.service_name,
        qty = 1,
        unit_price = seeded_booking_invoice.unit_price,
        amount = seeded_booking_invoice.total_amount
    FROM seeded_booking_invoices seeded_booking_invoice
    WHERE invoice_line.shop_id = seeded_booking_invoice.shop_id
      AND invoice_line.invoice_id = seeded_booking_invoice.invoice_id
      AND invoice_line.line_type = 'SERVICE'
      AND invoice_line.ref_id = seeded_booking_invoice.service_id
    RETURNING invoice_line.id
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
),
updated_booking_commissions AS (
    UPDATE platform_commissions commission
    SET gross_amount = seeded_booking.total_amount,
        discount_amount = 0,
        shipping_fee = 0,
        commission_base = seeded_booking.total_amount,
        commission_rate_bps = 1500,
        commission_amount = ROUND(seeded_booking.total_amount * 0.15)::bigint,
        status = 'PENDING'::commission_status
    FROM seeded_bookings seeded_booking
    WHERE commission.source_type = CASE
            WHEN seeded_booking.service_type = 'VETERINARY' THEN 'VET_BOOKING'::commission_source_type
            ELSE 'SERVICE_BOOKING'::commission_source_type
        END
      AND commission.source_id = seeded_booking.booking_id
    RETURNING commission.id
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
