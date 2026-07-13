WITH target_order_commissions AS (
    SELECT
        order_row.shop_id,
        'ORDER'::commission_source_type AS source_type,
        order_row.id AS source_id,
        order_row.subtotal_amount + order_row.shipping_fee AS gross_amount,
        order_row.discount_amount,
        order_row.shipping_fee,
        GREATEST(order_row.subtotal_amount, 0) AS commission_base,
        order_row.created_at
    FROM orders order_row
    WHERE order_row.shop_id = 8
      AND order_row.status = 'COMPLETED'::order_status
      AND order_row.order_code IN (
          'FAKE-S8-U120-P46-20260712',
          'FAKE-S8-U121-P47-20260712',
          'FAKE-S8-U122-P48-20260712'
      )
),
updated_order_commissions AS (
    UPDATE platform_commissions commission
    SET gross_amount = target.gross_amount,
        discount_amount = target.discount_amount,
        shipping_fee = target.shipping_fee,
        commission_base = target.commission_base,
        commission_rate_bps = 1500,
        commission_amount = ROUND(target.commission_base * 0.15)::bigint,
        status = 'PENDING'::commission_status
    FROM target_order_commissions target
    WHERE commission.source_type = target.source_type
      AND commission.source_id = target.source_id
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
    target.shop_id,
    target.source_type,
    target.source_id,
    target.gross_amount,
    target.discount_amount,
    target.shipping_fee,
    target.commission_base,
    1500,
    ROUND(target.commission_base * 0.15)::bigint,
    'PENDING'::commission_status,
    target.created_at
FROM target_order_commissions target
WHERE NOT EXISTS (
    SELECT 1
    FROM platform_commissions existing_commission
    WHERE existing_commission.source_type = target.source_type
      AND existing_commission.source_id = target.source_id
);

WITH target_booking_commissions AS (
    SELECT
        booking.shop_id,
        CASE
            WHEN COALESCE(service.service_type, 'GENERAL') = 'VETERINARY'
                THEN 'VET_BOOKING'::commission_source_type
            ELSE 'SERVICE_BOOKING'::commission_source_type
        END AS source_type,
        booking.id AS source_id,
        invoice.total_amount AS gross_amount,
        0::bigint AS discount_amount,
        0::bigint AS shipping_fee,
        GREATEST(invoice.total_amount, 0) AS commission_base,
        booking.start_at AS created_at
    FROM bookings booking
    JOIN booking_items booking_item
      ON booking_item.shop_id = booking.shop_id
     AND booking_item.booking_id = booking.id
     AND booking_item.item_type = 'SERVICE'::booking_item_type
    JOIN services service
      ON service.shop_id = booking.shop_id
     AND service.id = booking_item.ref_id
    JOIN LATERAL (
        SELECT COALESCE(existing_invoice.total_amount, booking_item.amount) AS total_amount
        FROM invoices existing_invoice
        WHERE existing_invoice.shop_id = booking.shop_id
          AND existing_invoice.booking_id = booking.id
        ORDER BY existing_invoice.id DESC
        LIMIT 1
    ) invoice ON true
    WHERE booking.shop_id = 8
      AND booking.status = 'COMPLETED'::booking_status
      AND booking.note IN (
          'V112_FAKE_COMPLETED_BOOKING_S8_U120_S86_20260712',
          'V112_FAKE_COMPLETED_BOOKING_S8_U121_S85_20260712'
      )
),
updated_booking_commissions AS (
    UPDATE platform_commissions commission
    SET gross_amount = target.gross_amount,
        discount_amount = target.discount_amount,
        shipping_fee = target.shipping_fee,
        commission_base = target.commission_base,
        commission_rate_bps = 1500,
        commission_amount = ROUND(target.commission_base * 0.15)::bigint,
        status = 'PENDING'::commission_status
    FROM target_booking_commissions target
    WHERE commission.source_type = target.source_type
      AND commission.source_id = target.source_id
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
    target.shop_id,
    target.source_type,
    target.source_id,
    target.gross_amount,
    target.discount_amount,
    target.shipping_fee,
    target.commission_base,
    1500,
    ROUND(target.commission_base * 0.15)::bigint,
    'PENDING'::commission_status,
    target.created_at
FROM target_booking_commissions target
WHERE NOT EXISTS (
    SELECT 1
    FROM platform_commissions existing_commission
    WHERE existing_commission.source_type = target.source_type
      AND existing_commission.source_id = target.source_id
);
