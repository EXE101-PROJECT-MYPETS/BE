WITH target_orders AS (
    SELECT
        order_row.id AS order_id,
        order_row.shop_id,
        order_row.user_id,
        order_row.customer_id,
        order_row.created_at,
        product.id AS product_id,
        product.name AS product_name,
        product.price AS unit_price
    FROM orders order_row
    JOIN products product
      ON product.shop_id = order_row.shop_id
     AND product.id = CASE
         WHEN order_row.order_code = 'FAKE-S8-U120-P46-20260712' THEN 46
         WHEN order_row.order_code = 'FAKE-S8-U121-P47-20260712' THEN 47
         WHEN order_row.order_code = 'FAKE-S8-U122-P48-20260712' THEN 48
     END
    WHERE order_row.shop_id = 8
      AND order_row.order_code IN (
          'FAKE-S8-U120-P46-20260712',
          'FAKE-S8-U121-P47-20260712',
          'FAKE-S8-U122-P48-20260712'
      )
),
updated_orders AS (
    UPDATE orders order_row
    SET subtotal_amount = target_order.unit_price,
        shipping_fee = 0,
        discount_amount = 0,
        total_amount = target_order.unit_price,
        status = 'COMPLETED'::order_status,
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
        amount = target_order.unit_price
    FROM target_orders target_order
    WHERE order_item.shop_id = target_order.shop_id
      AND order_item.order_id = target_order.order_id
      AND order_item.product_id = target_order.product_id
    RETURNING order_item.id
),
inserted_order_items AS (
    INSERT INTO order_items (shop_id, order_id, product_id, qty, unit_price, amount, created_at)
    SELECT
        target_order.shop_id,
        target_order.order_id,
        target_order.product_id,
        1,
        target_order.unit_price,
        target_order.unit_price,
        target_order.created_at
    FROM target_orders target_order
    WHERE NOT EXISTS (
        SELECT 1
        FROM order_items existing_item
        WHERE existing_item.shop_id = target_order.shop_id
          AND existing_item.order_id = target_order.order_id
          AND existing_item.product_id = target_order.product_id
    )
    RETURNING id
),
updated_order_invoices AS (
    UPDATE invoices invoice
    SET total_amount = target_order.unit_price,
        status = 'PAID'::invoice_status,
        payment_method = 'CASH',
        issued_at = COALESCE(invoice.issued_at, target_order.created_at),
        updated_at = target_order.created_at
    FROM target_orders target_order
    WHERE invoice.shop_id = target_order.shop_id
      AND invoice.order_id = target_order.order_id
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
        target_order.shop_id,
        target_order.user_id,
        target_order.customer_id,
        target_order.order_id,
        target_order.unit_price,
        'PAID'::invoice_status,
        'CASH',
        target_order.created_at,
        target_order.created_at,
        target_order.created_at
    FROM target_orders target_order
    WHERE NOT EXISTS (
        SELECT 1
        FROM invoices existing_invoice
        WHERE existing_invoice.shop_id = target_order.shop_id
          AND existing_invoice.order_id = target_order.order_id
    )
    RETURNING id, shop_id, order_id
),
target_order_invoices AS (
    SELECT invoice_row.id AS invoice_id,
           target_order.shop_id,
           target_order.product_id,
           target_order.product_name,
           target_order.unit_price
    FROM target_orders target_order
    JOIN (
        SELECT id, shop_id, order_id FROM updated_order_invoices
        UNION ALL
        SELECT id, shop_id, order_id FROM inserted_order_invoices
    ) invoice_row
      ON invoice_row.shop_id = target_order.shop_id
     AND invoice_row.order_id = target_order.order_id
),
updated_order_invoice_lines AS (
    UPDATE invoice_lines invoice_line
    SET item_name = target_invoice.product_name,
        qty = 1,
        unit_price = target_invoice.unit_price,
        amount = target_invoice.unit_price
    FROM target_order_invoices target_invoice
    WHERE invoice_line.shop_id = target_invoice.shop_id
      AND invoice_line.invoice_id = target_invoice.invoice_id
      AND invoice_line.line_type = 'PRODUCT'
      AND invoice_line.ref_id = target_invoice.product_id
    RETURNING invoice_line.id
),
inserted_order_invoice_lines AS (
    INSERT INTO invoice_lines (shop_id, invoice_id, line_type, ref_id, item_name, qty, unit_price, amount)
    SELECT
        target_invoice.shop_id,
        target_invoice.invoice_id,
        'PRODUCT',
        target_invoice.product_id,
        target_invoice.product_name,
        1,
        target_invoice.unit_price,
        target_invoice.unit_price
    FROM target_order_invoices target_invoice
    WHERE NOT EXISTS (
        SELECT 1
        FROM invoice_lines existing_line
        WHERE existing_line.shop_id = target_invoice.shop_id
          AND existing_line.invoice_id = target_invoice.invoice_id
          AND existing_line.line_type = 'PRODUCT'
          AND existing_line.ref_id = target_invoice.product_id
    )
    RETURNING id
)
UPDATE platform_commissions commission
SET gross_amount = target_order.unit_price,
    discount_amount = 0,
    shipping_fee = 0,
    commission_base = target_order.unit_price,
    commission_amount = ROUND(target_order.unit_price * 0.15)::bigint
FROM target_orders target_order
WHERE commission.source_type = 'ORDER'::commission_source_type
  AND commission.source_id = target_order.order_id;

WITH target_bookings AS (
    SELECT
        booking.id AS booking_id,
        booking.shop_id,
        booking.user_id,
        booking.customer_id,
        booking.start_at,
        service.id AS service_id,
        service.name AS service_name,
        service.base_price AS unit_price,
        COALESCE(service.service_type, 'GENERAL') AS service_type
    FROM bookings booking
    JOIN services service
      ON service.shop_id = booking.shop_id
     AND service.id = CASE
         WHEN booking.note = 'V112_FAKE_COMPLETED_BOOKING_S8_U120_S86_20260712' THEN 86
         WHEN booking.note = 'V112_FAKE_COMPLETED_BOOKING_S8_U121_S85_20260712' THEN 85
     END
    WHERE booking.shop_id = 8
      AND booking.note IN (
          'V112_FAKE_COMPLETED_BOOKING_S8_U120_S86_20260712',
          'V112_FAKE_COMPLETED_BOOKING_S8_U121_S85_20260712'
      )
),
updated_bookings AS (
    UPDATE bookings booking
    SET status = 'COMPLETED'::booking_status,
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
        amount = target_booking.unit_price
    FROM target_bookings target_booking
    WHERE booking_item.shop_id = target_booking.shop_id
      AND booking_item.booking_id = target_booking.booking_id
      AND booking_item.item_type = 'SERVICE'::booking_item_type
      AND booking_item.ref_id = target_booking.service_id
    RETURNING booking_item.id
),
inserted_booking_items AS (
    INSERT INTO booking_items (shop_id, booking_id, item_type, ref_id, qty, unit_price, amount, created_at)
    SELECT
        target_booking.shop_id,
        target_booking.booking_id,
        'SERVICE'::booking_item_type,
        target_booking.service_id,
        1,
        target_booking.unit_price,
        target_booking.unit_price,
        target_booking.start_at
    FROM target_bookings target_booking
    WHERE NOT EXISTS (
        SELECT 1
        FROM booking_items existing_item
        WHERE existing_item.shop_id = target_booking.shop_id
          AND existing_item.booking_id = target_booking.booking_id
          AND existing_item.item_type = 'SERVICE'::booking_item_type
          AND existing_item.ref_id = target_booking.service_id
    )
    RETURNING id
),
updated_booking_invoices AS (
    UPDATE invoices invoice
    SET total_amount = target_booking.unit_price,
        status = 'PAID'::invoice_status,
        payment_method = 'CASH',
        issued_at = COALESCE(invoice.issued_at, target_booking.start_at),
        updated_at = target_booking.start_at
    FROM target_bookings target_booking
    WHERE invoice.shop_id = target_booking.shop_id
      AND invoice.booking_id = target_booking.booking_id
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
        target_booking.shop_id,
        target_booking.user_id,
        target_booking.customer_id,
        target_booking.booking_id,
        target_booking.unit_price,
        'PAID'::invoice_status,
        'CASH',
        target_booking.start_at,
        target_booking.start_at,
        target_booking.start_at
    FROM target_bookings target_booking
    WHERE NOT EXISTS (
        SELECT 1
        FROM invoices existing_invoice
        WHERE existing_invoice.shop_id = target_booking.shop_id
          AND existing_invoice.booking_id = target_booking.booking_id
    )
    RETURNING id, shop_id, booking_id
),
target_booking_invoices AS (
    SELECT invoice_row.id AS invoice_id,
           target_booking.shop_id,
           target_booking.service_id,
           target_booking.service_name,
           target_booking.unit_price
    FROM target_bookings target_booking
    JOIN (
        SELECT id, shop_id, booking_id FROM updated_booking_invoices
        UNION ALL
        SELECT id, shop_id, booking_id FROM inserted_booking_invoices
    ) invoice_row
      ON invoice_row.shop_id = target_booking.shop_id
     AND invoice_row.booking_id = target_booking.booking_id
),
updated_booking_invoice_lines AS (
    UPDATE invoice_lines invoice_line
    SET item_name = target_invoice.service_name,
        qty = 1,
        unit_price = target_invoice.unit_price,
        amount = target_invoice.unit_price
    FROM target_booking_invoices target_invoice
    WHERE invoice_line.shop_id = target_invoice.shop_id
      AND invoice_line.invoice_id = target_invoice.invoice_id
      AND invoice_line.line_type = 'SERVICE'
      AND invoice_line.ref_id = target_invoice.service_id
    RETURNING invoice_line.id
),
inserted_booking_invoice_lines AS (
    INSERT INTO invoice_lines (shop_id, invoice_id, line_type, ref_id, item_name, qty, unit_price, amount)
    SELECT
        target_invoice.shop_id,
        target_invoice.invoice_id,
        'SERVICE',
        target_invoice.service_id,
        target_invoice.service_name,
        1,
        target_invoice.unit_price,
        target_invoice.unit_price
    FROM target_booking_invoices target_invoice
    WHERE NOT EXISTS (
        SELECT 1
        FROM invoice_lines existing_line
        WHERE existing_line.shop_id = target_invoice.shop_id
          AND existing_line.invoice_id = target_invoice.invoice_id
          AND existing_line.line_type = 'SERVICE'
          AND existing_line.ref_id = target_invoice.service_id
    )
    RETURNING id
)
UPDATE platform_commissions commission
SET gross_amount = target_booking.unit_price,
    discount_amount = 0,
    shipping_fee = 0,
    commission_base = target_booking.unit_price,
    commission_amount = ROUND(target_booking.unit_price * 0.15)::bigint
FROM target_bookings target_booking
WHERE commission.source_type = CASE
        WHEN target_booking.service_type = 'VETERINARY' THEN 'VET_BOOKING'::commission_source_type
        ELSE 'SERVICE_BOOKING'::commission_source_type
    END
  AND commission.source_id = target_booking.booking_id;
