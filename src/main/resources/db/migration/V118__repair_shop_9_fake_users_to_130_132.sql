DROP TABLE IF EXISTS seed_shop_9_repair_users;

CREATE TEMP TABLE seed_shop_9_repair_users ON COMMIT DROP AS
SELECT
    seed_user.seed_slot,
    app_user.id,
    app_user.full_name,
    app_user.phone,
    app_user.email,
    app_user.address
FROM (
    VALUES
        (1::bigint, 130::bigint),
        (2::bigint, 131::bigint),
        (3::bigint, 132::bigint)
) AS seed_user(seed_slot, user_id)
JOIN users app_user
  ON app_user.id = seed_user.user_id
WHERE EXISTS (
    SELECT 1
    FROM shops shop
    WHERE shop.id = 9
);

UPDATE customers customer
SET user_id = target_user.id,
    full_name = COALESCE(NULLIF(btrim(target_user.full_name), ''), customer.full_name),
    phone = COALESCE(NULLIF(btrim(customer.phone), ''), NULLIF(btrim(target_user.phone), '')),
    updated_at = now()
FROM seed_shop_9_repair_users target_user
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
FROM seed_shop_9_repair_users target_user
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

WITH order_slots AS (
    SELECT
        order_row.id AS order_id,
        order_row.shop_id,
        order_row.created_at,
        ROW_NUMBER() OVER (ORDER BY order_row.created_at, order_row.id) AS seed_slot,
        order_item.product_id
    FROM orders order_row
    JOIN LATERAL (
        SELECT item.product_id
        FROM order_items item
        WHERE item.shop_id = order_row.shop_id
          AND item.order_id = order_row.id
        ORDER BY item.id
        LIMIT 1
    ) order_item ON true
    WHERE order_row.shop_id = 9
      AND order_row.note = 'V117_FAKE_COMPLETED_PRODUCT_ORDER'
      AND order_row.created_at >= '2026-07-12 00:00:00+07'::timestamptz
      AND order_row.created_at < '2026-07-13 00:00:00+07'::timestamptz
),
target_orders AS (
    SELECT
        order_slot.order_id,
        order_slot.shop_id,
        target_user.id AS user_id,
        customer.id AS customer_id,
        product.id AS product_id,
        product.name AS product_name,
        product.price AS unit_price,
        product.price AS total_amount,
        order_slot.created_at,
        'FAKE-S9-U' || target_user.id || '-P' || product.id || '-' || TO_CHAR(order_slot.created_at, 'YYYYMMDD') AS order_code,
        COALESCE(NULLIF(btrim(address.name), ''), NULLIF(btrim(target_user.full_name), ''), 'Khach hang ' || target_user.id) AS receiver_name,
        COALESCE(NULLIF(btrim(address.tel), ''), NULLIF(btrim(target_user.phone), ''), '0900000' || target_user.id) AS receiver_phone,
        COALESCE(NULLIF(btrim(address.address), ''), NULLIF(btrim(target_user.address), ''), 'Dia chi demo') AS shipping_address,
        COALESCE(NULLIF(btrim(address.province), ''), 'TP. Ho Chi Minh') AS shipping_province,
        COALESCE(NULLIF(btrim(address.district), ''), 'Quan 1') AS shipping_district,
        COALESCE(NULLIF(btrim(address.ward), ''), 'Phuong Ben Nghe') AS shipping_ward,
        COALESCE(NULLIF(btrim(address.hamlet), ''), 'Khac') AS shipping_hamlet,
        address.id AS user_address_id
    FROM order_slots order_slot
    JOIN seed_shop_9_repair_users target_user
      ON target_user.seed_slot = ((order_slot.seed_slot - 1) % 3) + 1
    JOIN products product
      ON product.shop_id = order_slot.shop_id
     AND product.id = order_slot.product_id
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
updated_orders AS (
    UPDATE orders order_row
    SET user_id = target_order.user_id,
        customer_id = target_order.customer_id,
        user_address_id = target_order.user_address_id,
        order_code = CASE
            WHEN NOT EXISTS (
                SELECT 1
                FROM orders code_conflict
                WHERE code_conflict.shop_id = target_order.shop_id
                  AND code_conflict.order_code = target_order.order_code
                  AND code_conflict.id <> target_order.order_id
            ) THEN target_order.order_code
            ELSE order_row.order_code
        END,
        status = 'COMPLETED'::order_status,
        subtotal_amount = target_order.total_amount,
        shipping_fee = 0,
        discount_amount = 0,
        total_amount = target_order.total_amount,
        receiver_name = target_order.receiver_name,
        receiver_phone = target_order.receiver_phone,
        shipping_address = target_order.shipping_address,
        shipping_province = target_order.shipping_province,
        shipping_district = target_order.shipping_district,
        shipping_ward = target_order.shipping_ward,
        shipping_hamlet = target_order.shipping_hamlet,
        updated_at = target_order.created_at
    FROM target_orders target_order
    WHERE order_row.shop_id = target_order.shop_id
      AND order_row.id = target_order.order_id
    RETURNING order_row.id
),
updated_order_items AS (
    UPDATE order_items order_item
    SET qty = 1,
        unit_price = target_order.unit_price,
        amount = target_order.total_amount
    FROM target_orders target_order
    WHERE order_item.shop_id = target_order.shop_id
      AND order_item.order_id = target_order.order_id
      AND order_item.product_id = target_order.product_id
    RETURNING order_item.id
),
updated_order_invoices AS (
    UPDATE invoices invoice
    SET user_id = target_order.user_id,
        customer_id = target_order.customer_id,
        total_amount = target_order.total_amount,
        status = 'PAID'::invoice_status,
        payment_method = 'CASH',
        issued_at = target_order.created_at,
        updated_at = target_order.created_at
    FROM target_orders target_order
    WHERE invoice.shop_id = target_order.shop_id
      AND invoice.order_id = target_order.order_id
    RETURNING invoice.id, invoice.shop_id, invoice.order_id
),
target_order_invoice_lines AS (
    SELECT
        invoice.id AS invoice_id,
        target_order.shop_id,
        target_order.product_id,
        target_order.product_name,
        target_order.unit_price,
        target_order.total_amount
    FROM target_orders target_order
    JOIN invoices invoice
      ON invoice.shop_id = target_order.shop_id
     AND invoice.order_id = target_order.order_id
)
UPDATE invoice_lines invoice_line
SET item_name = target_invoice.product_name,
    qty = 1,
    unit_price = target_invoice.unit_price,
    amount = target_invoice.total_amount
FROM target_order_invoice_lines target_invoice
WHERE invoice_line.shop_id = target_invoice.shop_id
  AND invoice_line.invoice_id = target_invoice.invoice_id
  AND invoice_line.line_type = 'PRODUCT'
  AND invoice_line.ref_id = target_invoice.product_id;

WITH booking_slots AS (
    SELECT
        booking.id AS booking_id,
        booking.shop_id,
        booking.start_at,
        ROW_NUMBER() OVER (ORDER BY booking.start_at, booking.id) AS seed_slot,
        booking_item.ref_id AS service_id
    FROM bookings booking
    JOIN LATERAL (
        SELECT item.ref_id
        FROM booking_items item
        WHERE item.shop_id = booking.shop_id
          AND item.booking_id = booking.id
          AND item.item_type = 'SERVICE'::booking_item_type
        ORDER BY item.id
        LIMIT 1
    ) booking_item ON true
    WHERE booking.shop_id = 9
      AND (
          booking.note LIKE 'V117_FAKE_COMPLETED_BOOKING_S9_%'
          OR booking.note LIKE 'V118_FAKE_COMPLETED_BOOKING_S9_%'
      )
      AND booking.start_at >= '2026-07-12 00:00:00+07'::timestamptz
      AND booking.start_at < '2026-07-15 00:00:00+07'::timestamptz
),
target_bookings AS (
    SELECT
        booking_slot.booking_id,
        booking_slot.shop_id,
        target_user.id AS user_id,
        customer.id AS customer_id,
        service.id AS service_id,
        service.name AS service_name,
        service.base_price AS unit_price,
        service.base_price AS total_amount,
        COALESCE(service.service_type, 'GENERAL') AS service_type,
        booking_slot.start_at,
        'V118_FAKE_COMPLETED_BOOKING_S9_U' || target_user.id || '_S' || service.id || '_' || TO_CHAR(booking_slot.start_at, 'YYYYMMDD') AS note
    FROM booking_slots booking_slot
    JOIN seed_shop_9_repair_users target_user
      ON target_user.seed_slot = ((booking_slot.seed_slot - 1) % 3) + 1
    JOIN services service
      ON service.shop_id = booking_slot.shop_id
     AND service.id = booking_slot.service_id
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
updated_bookings AS (
    UPDATE bookings booking
    SET user_id = target_booking.user_id,
        customer_id = target_booking.customer_id,
        status = 'COMPLETED'::booking_status,
        note = target_booking.note,
        updated_at = target_booking.start_at
    FROM target_bookings target_booking
    WHERE booking.shop_id = target_booking.shop_id
      AND booking.id = target_booking.booking_id
    RETURNING booking.id
),
updated_booking_items AS (
    UPDATE booking_items booking_item
    SET qty = 1,
        unit_price = target_booking.unit_price,
        amount = target_booking.total_amount
    FROM target_bookings target_booking
    WHERE booking_item.shop_id = target_booking.shop_id
      AND booking_item.booking_id = target_booking.booking_id
      AND booking_item.item_type = 'SERVICE'::booking_item_type
      AND booking_item.ref_id = target_booking.service_id
    RETURNING booking_item.id
),
updated_booking_invoices AS (
    UPDATE invoices invoice
    SET user_id = target_booking.user_id,
        customer_id = target_booking.customer_id,
        total_amount = target_booking.total_amount,
        status = 'PAID'::invoice_status,
        payment_method = 'CASH',
        issued_at = target_booking.start_at,
        updated_at = target_booking.start_at
    FROM target_bookings target_booking
    WHERE invoice.shop_id = target_booking.shop_id
      AND invoice.booking_id = target_booking.booking_id
    RETURNING invoice.id, invoice.shop_id, invoice.booking_id
),
target_booking_invoice_lines AS (
    SELECT
        invoice.id AS invoice_id,
        target_booking.shop_id,
        target_booking.service_id,
        target_booking.service_name,
        target_booking.unit_price,
        target_booking.total_amount
    FROM target_bookings target_booking
    JOIN invoices invoice
      ON invoice.shop_id = target_booking.shop_id
     AND invoice.booking_id = target_booking.booking_id
)
UPDATE invoice_lines invoice_line
SET item_name = target_invoice.service_name,
    qty = 1,
    unit_price = target_invoice.unit_price,
    amount = target_invoice.total_amount
FROM target_booking_invoice_lines target_invoice
WHERE invoice_line.shop_id = target_invoice.shop_id
  AND invoice_line.invoice_id = target_invoice.invoice_id
  AND invoice_line.line_type = 'SERVICE'
  AND invoice_line.ref_id = target_invoice.service_id;
