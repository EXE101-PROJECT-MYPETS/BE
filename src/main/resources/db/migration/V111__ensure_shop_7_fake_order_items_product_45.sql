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
     AND product.id = 45
    WHERE order_row.shop_id = 7
      AND order_row.order_code IN (
          'FAKE-S7-U68-P45-20260710',
          'FAKE-S7-U69-P45-20260711',
          'FAKE-S7-U70-P45-20260712'
      )
),
deleted_wrong_order_items AS (
    DELETE FROM order_items order_item
    USING target_orders target_order
    WHERE order_item.shop_id = target_order.shop_id
      AND order_item.order_id = target_order.order_id
      AND order_item.product_id <> 45
    RETURNING order_item.id
),
updated_order_items AS (
    UPDATE order_items order_item
    SET qty = 1,
        unit_price = target_order.unit_price,
        amount = target_order.unit_price
    FROM target_orders target_order
    WHERE order_item.shop_id = target_order.shop_id
      AND order_item.order_id = target_order.order_id
      AND order_item.product_id = 45
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
          AND existing_item.product_id = 45
    )
    RETURNING id
),
updated_orders AS (
    UPDATE orders order_row
    SET subtotal_amount = target_order.unit_price,
        shipping_fee = 0,
        discount_amount = 0,
        total_amount = target_order.unit_price,
        updated_at = target_order.created_at
    FROM target_orders target_order
    WHERE order_row.shop_id = target_order.shop_id
      AND order_row.id = target_order.order_id
    RETURNING order_row.id
),
updated_invoices AS (
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
inserted_invoices AS (
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
target_invoices AS (
    SELECT invoice_row.id AS invoice_id,
           invoice_row.shop_id,
           target_order.order_id,
           target_order.product_id,
           target_order.product_name,
           target_order.unit_price
    FROM target_orders target_order
    JOIN (
        SELECT id, shop_id, order_id
        FROM updated_invoices

        UNION ALL

        SELECT id, shop_id, order_id
        FROM inserted_invoices
    ) invoice_row
      ON invoice_row.shop_id = target_order.shop_id
     AND invoice_row.order_id = target_order.order_id
),
deleted_wrong_invoice_lines AS (
    DELETE FROM invoice_lines invoice_line
    USING target_invoices target_invoice
    WHERE invoice_line.shop_id = target_invoice.shop_id
      AND invoice_line.invoice_id = target_invoice.invoice_id
      AND (
          invoice_line.line_type <> 'PRODUCT'
          OR invoice_line.ref_id <> 45
      )
    RETURNING invoice_line.id
),
updated_invoice_lines AS (
    UPDATE invoice_lines invoice_line
    SET item_name = target_invoice.product_name,
        qty = 1,
        unit_price = target_invoice.unit_price,
        amount = target_invoice.unit_price
    FROM target_invoices target_invoice
    WHERE invoice_line.shop_id = target_invoice.shop_id
      AND invoice_line.invoice_id = target_invoice.invoice_id
      AND invoice_line.line_type = 'PRODUCT'
      AND invoice_line.ref_id = 45
    RETURNING invoice_line.id
),
inserted_invoice_lines AS (
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
    FROM target_invoices target_invoice
    WHERE NOT EXISTS (
        SELECT 1
        FROM invoice_lines existing_line
        WHERE existing_line.shop_id = target_invoice.shop_id
          AND existing_line.invoice_id = target_invoice.invoice_id
          AND existing_line.line_type = 'PRODUCT'
          AND existing_line.ref_id = 45
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
