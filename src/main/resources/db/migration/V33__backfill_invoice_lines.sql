INSERT INTO invoice_lines (shop_id, invoice_id, line_type, ref_id, item_name, qty, unit_price, amount)
SELECT
    invoices.shop_id,
    invoices.id,
    'PRODUCT',
    order_items.product_id,
    COALESCE(products.name, 'Product #' || order_items.product_id),
    order_items.qty,
    order_items.unit_price,
    order_items.amount
FROM invoices
JOIN order_items
    ON order_items.shop_id = invoices.shop_id
    AND order_items.order_id = invoices.order_id
LEFT JOIN products
    ON products.shop_id = order_items.shop_id
    AND products.id = order_items.product_id
WHERE invoices.order_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM invoice_lines existing_lines
      WHERE existing_lines.invoice_id = invoices.id
  );

INSERT INTO invoice_lines (shop_id, invoice_id, line_type, ref_id, item_name, qty, unit_price, amount)
SELECT
    invoices.shop_id,
    invoices.id,
    booking_items.item_type::text,
    booking_items.ref_id,
    COALESCE(
        CASE
            WHEN booking_items.item_type::text = 'PRODUCT' THEN products.name
            WHEN booking_items.item_type::text = 'SERVICE' THEN services.name
            ELSE NULL
        END,
        booking_items.item_type::text || COALESCE(' #' || booking_items.ref_id, '')
    ),
    booking_items.qty,
    booking_items.unit_price,
    booking_items.amount
FROM invoices
JOIN booking_items
    ON booking_items.shop_id = invoices.shop_id
    AND booking_items.booking_id = invoices.booking_id
LEFT JOIN products
    ON products.shop_id = booking_items.shop_id
    AND products.id = booking_items.ref_id
LEFT JOIN services
    ON services.shop_id = booking_items.shop_id
    AND services.id = booking_items.ref_id
WHERE invoices.booking_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM invoice_lines existing_lines
      WHERE existing_lines.invoice_id = invoices.id
  );
