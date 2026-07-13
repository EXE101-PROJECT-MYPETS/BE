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
     AND service.id = CASE booking.note
         WHEN 'V115_FAKE_COMPLETED_BOOKING_S8_U122_S86_20260712' THEN 86
         WHEN 'V115_FAKE_COMPLETED_BOOKING_S8_U120_S86_20260714' THEN 86
         WHEN 'V115_FAKE_COMPLETED_BOOKING_S8_U121_S85_20260714' THEN 85
         WHEN 'V115_FAKE_COMPLETED_BOOKING_S8_U122_S86_20260714' THEN 86
     END
    WHERE booking.shop_id = 8
      AND booking.note IN (
          'V115_FAKE_COMPLETED_BOOKING_S8_U122_S86_20260712',
          'V115_FAKE_COMPLETED_BOOKING_S8_U120_S86_20260714',
          'V115_FAKE_COMPLETED_BOOKING_S8_U121_S85_20260714',
          'V115_FAKE_COMPLETED_BOOKING_S8_U122_S86_20260714'
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
deleted_wrong_items AS (
    DELETE FROM booking_items booking_item
    USING target_bookings target_booking
    WHERE booking_item.shop_id = target_booking.shop_id
      AND booking_item.booking_id = target_booking.booking_id
      AND (
          booking_item.item_type <> 'SERVICE'::booking_item_type
          OR booking_item.ref_id <> target_booking.service_id
      )
    RETURNING booking_item.id
),
updated_items AS (
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
inserted_items AS (
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
updated_invoices AS (
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
inserted_invoices AS (
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
target_invoices AS (
    SELECT
        invoice_row.id AS invoice_id,
        target_booking.shop_id,
        target_booking.booking_id,
        target_booking.service_id,
        target_booking.service_name,
        target_booking.unit_price
    FROM target_bookings target_booking
    JOIN (
        SELECT id, shop_id, booking_id FROM updated_invoices
        UNION ALL
        SELECT id, shop_id, booking_id FROM inserted_invoices
    ) invoice_row
      ON invoice_row.shop_id = target_booking.shop_id
     AND invoice_row.booking_id = target_booking.booking_id
),
deleted_wrong_invoice_lines AS (
    DELETE FROM invoice_lines invoice_line
    USING target_invoices target_invoice
    WHERE invoice_line.shop_id = target_invoice.shop_id
      AND invoice_line.invoice_id = target_invoice.invoice_id
      AND (
          invoice_line.line_type <> 'SERVICE'
          OR invoice_line.ref_id <> target_invoice.service_id
      )
    RETURNING invoice_line.id
),
updated_invoice_lines AS (
    UPDATE invoice_lines invoice_line
    SET item_name = target_invoice.service_name,
        qty = 1,
        unit_price = target_invoice.unit_price,
        amount = target_invoice.unit_price
    FROM target_invoices target_invoice
    WHERE invoice_line.shop_id = target_invoice.shop_id
      AND invoice_line.invoice_id = target_invoice.invoice_id
      AND invoice_line.line_type = 'SERVICE'
      AND invoice_line.ref_id = target_invoice.service_id
    RETURNING invoice_line.id
),
inserted_invoice_lines AS (
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
    FROM target_invoices target_invoice
    WHERE NOT EXISTS (
        SELECT 1
        FROM invoice_lines existing_line
        WHERE existing_line.shop_id = target_invoice.shop_id
          AND existing_line.invoice_id = target_invoice.invoice_id
          AND existing_line.line_type = 'SERVICE'
          AND existing_line.ref_id = target_invoice.service_id
    )
    RETURNING id
),
target_commissions AS (
    SELECT
        target_booking.shop_id,
        CASE
            WHEN target_booking.service_type = 'VETERINARY' THEN 'VET_BOOKING'::commission_source_type
            ELSE 'SERVICE_BOOKING'::commission_source_type
        END AS source_type,
        target_booking.booking_id AS source_id,
        target_booking.unit_price AS gross_amount,
        0::bigint AS discount_amount,
        0::bigint AS shipping_fee,
        target_booking.unit_price AS commission_base,
        target_booking.start_at AS created_at
    FROM target_bookings target_booking
),
updated_commissions AS (
    UPDATE platform_commissions commission
    SET gross_amount = target_commission.gross_amount,
        discount_amount = target_commission.discount_amount,
        shipping_fee = target_commission.shipping_fee,
        commission_base = target_commission.commission_base,
        commission_rate_bps = 1500,
        commission_amount = ROUND(target_commission.commission_base * 0.15)::bigint,
        status = 'PENDING'::commission_status
    FROM target_commissions target_commission
    WHERE commission.source_type = target_commission.source_type
      AND commission.source_id = target_commission.source_id
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
    target_commission.shop_id,
    target_commission.source_type,
    target_commission.source_id,
    target_commission.gross_amount,
    target_commission.discount_amount,
    target_commission.shipping_fee,
    target_commission.commission_base,
    1500,
    ROUND(target_commission.commission_base * 0.15)::bigint,
    'PENDING'::commission_status,
    target_commission.created_at
FROM target_commissions target_commission
WHERE NOT EXISTS (
    SELECT 1
    FROM platform_commissions existing_commission
    WHERE existing_commission.source_type = target_commission.source_type
      AND existing_commission.source_id = target_commission.source_id
);
