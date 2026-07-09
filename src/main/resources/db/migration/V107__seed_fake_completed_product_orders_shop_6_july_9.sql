WITH seed_products AS (
    SELECT *
    FROM (
        VALUES
            (35::bigint, '2026-07-09 09:15:00+07'::timestamptz),
            (36::bigint, '2026-07-09 13:30:00+07'::timestamptz),
            (37::bigint, '2026-07-09 18:45:00+07'::timestamptz)
    ) AS seed(product_id, completed_at)
),
seed_context AS (
    SELECT
        6::bigint AS shop_id,
        62::bigint AS user_id,
        customer.id AS customer_id,
        COALESCE(NULLIF(btrim(address.name), ''), NULLIF(btrim(app_user.full_name), ''), 'Khach hang 62') AS receiver_name,
        COALESCE(NULLIF(btrim(address.tel), ''), NULLIF(btrim(app_user.phone), ''), '0900000062') AS receiver_phone,
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
        WHERE c.shop_id = 6
          AND c.user_id = 62
        ORDER BY c.id
        LIMIT 1
    ) customer ON true
    LEFT JOIN LATERAL (
        SELECT ua.*
        FROM user_addresses ua
        WHERE ua.user_id = 62
        ORDER BY ua.is_default DESC, ua.id
        LIMIT 1
    ) address ON true
    WHERE app_user.id = 62
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
        'FAKE-S6-U62-P' || product.id || '-20260709' AS order_code,
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
     AND product.shop_id = 6
    CROSS JOIN seed_context context
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
        'V107_FAKE_COMPLETED_PRODUCT_ORDER',
        completed_at,
        completed_at
    FROM orders_to_seed
    WHERE NOT EXISTS (
        SELECT 1
        FROM orders existing_order
        WHERE existing_order.shop_id = orders_to_seed.shop_id
          AND existing_order.order_code = orders_to_seed.order_code
    )
    RETURNING id, shop_id, user_id, customer_id, order_code
),
existing_orders AS (
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
    WHERE NOT EXISTS (
        SELECT 1
        FROM inserted_orders inserted_order
        WHERE inserted_order.shop_id = order_row.shop_id
          AND inserted_order.order_code = order_row.order_code
    )
),
seeded_orders AS (
    SELECT
        inserted_order.id AS order_id,
        seed_order.shop_id,
        seed_order.user_id,
        seed_order.customer_id,
        seed_order.product_id,
        seed_order.product_name,
        seed_order.unit_price,
        seed_order.total_amount,
        seed_order.completed_at
    FROM inserted_orders inserted_order
    JOIN orders_to_seed seed_order
      ON seed_order.shop_id = inserted_order.shop_id
     AND seed_order.order_code = inserted_order.order_code

    UNION ALL

    SELECT
        order_id,
        shop_id,
        user_id,
        customer_id,
        product_id,
        product_name,
        unit_price,
        total_amount,
        completed_at
    FROM existing_orders
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
seeded_invoices AS (
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
        FROM inserted_invoices

        UNION ALL

        SELECT existing_invoice.id, existing_invoice.shop_id, existing_invoice.order_id
        FROM invoices existing_invoice
        JOIN seeded_orders seeded_order
          ON seeded_order.shop_id = existing_invoice.shop_id
         AND seeded_order.order_id = existing_invoice.order_id
        WHERE NOT EXISTS (
            SELECT 1
            FROM inserted_invoices inserted_invoice
            WHERE inserted_invoice.shop_id = existing_invoice.shop_id
              AND inserted_invoice.order_id = existing_invoice.order_id
        )
    ) invoice_row
      ON invoice_row.shop_id = seeded_orders.shop_id
     AND invoice_row.order_id = seeded_orders.order_id
),
inserted_invoice_lines AS (
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
    FROM seeded_invoices
    WHERE NOT EXISTS (
        SELECT 1
        FROM invoice_lines existing_line
        WHERE existing_line.shop_id = seeded_invoices.shop_id
          AND existing_line.invoice_id = seeded_invoices.invoice_id
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
