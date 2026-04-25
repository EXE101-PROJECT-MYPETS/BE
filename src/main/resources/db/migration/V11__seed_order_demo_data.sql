DELETE FROM booking_items
USING bookings
WHERE booking_items.booking_id = bookings.id
  AND booking_items.item_type = 'PRODUCT'::booking_item_type
  AND bookings.note LIKE 'V8_DEMO_BOOKING_%';

INSERT INTO booking_items (shop_id, booking_id, item_type, ref_id, qty, unit_price, amount)
SELECT
    bookings.shop_id,
    bookings.id,
    'SERVICE'::booking_item_type,
    services.id,
    seed_items.qty,
    services.base_price,
    services.base_price * seed_items.qty
FROM (
    VALUES
        ('V8_DEMO_BOOKING_001', 'Tam co ban', 1),
        ('V8_DEMO_BOOKING_001', 'Cat tia co ban', 1),
        ('V8_DEMO_BOOKING_002', 'Tam duong long', 1),
        ('V8_DEMO_BOOKING_003', 'Grooming toan dien', 1),
        ('V8_DEMO_BOOKING_004', 'Cat tia tao kieu', 1),
        ('V8_DEMO_BOOKING_005', 'Tam khu mui', 1),
        ('V8_DEMO_BOOKING_006', 'Tam spa nhe', 1),
        ('V8_DEMO_BOOKING_006', 'Chai long roi', 1),
        ('V8_DEMO_BOOKING_007', 'Cao long mua he', 1),
        ('V8_DEMO_BOOKING_008', 'Tam cham soc da', 1),
        ('V8_DEMO_BOOKING_008', 'Cat tia nhanh', 1),
        ('V8_DEMO_BOOKING_009', 'Tao kieu cao cap', 1),
        ('V8_DEMO_BOOKING_010', 'Tam nhanh', 1),
        ('V8_DEMO_BOOKING_011', 'Cat long theo yeu cau', 1),
        ('V8_DEMO_BOOKING_012', 'Tam ket hop massage', 1),
        ('V8_DEMO_BOOKING_013', 'Grooming toan dien', 1),
        ('V8_DEMO_BOOKING_014', 'Tam duong am', 1),
        ('V8_DEMO_BOOKING_015', 'Tam co ban', 1),
        ('V8_DEMO_BOOKING_015', 'Cat tia co ban', 1),
        ('V8_DEMO_BOOKING_016', 'Cat long nghe thuat', 1),
        ('V8_DEMO_BOOKING_017', 'Tam thu cung cao cap', 1),
        ('V8_DEMO_BOOKING_018', 'Tam trang long', 1),
        ('V8_DEMO_BOOKING_019', 'Cham soc long chuyen sau', 1),
        ('V8_DEMO_BOOKING_020', 'Cao long mua he', 1),
        ('V8_DEMO_BOOKING_021', 'Tam nhanh', 1),
        ('V8_DEMO_BOOKING_022', 'Tam duong long', 1),
        ('V8_DEMO_BOOKING_023', 'Cat tia nhanh', 1),
        ('V8_DEMO_BOOKING_024', 'Grooming toan dien', 1),
        ('V8_DEMO_BOOKING_025', 'Tam co ban', 1)
) AS seed_items(booking_note, service_name, qty)
JOIN bookings
    ON bookings.note = seed_items.booking_note
JOIN services
    ON services.shop_id = bookings.shop_id
    AND services.name = seed_items.service_name
WHERE NOT EXISTS (
    SELECT 1
    FROM booking_items
    WHERE booking_items.booking_id = bookings.id
      AND booking_items.item_type = 'SERVICE'::booking_item_type
      AND booking_items.ref_id = services.id
);

UPDATE invoices
SET total_amount = booking_totals.total_amount
FROM (
    SELECT
        booking_items.shop_id,
        booking_items.booking_id,
        COALESCE(SUM(booking_items.amount), 0) AS total_amount
    FROM booking_items
    JOIN bookings
        ON bookings.shop_id = booking_items.shop_id
        AND bookings.id = booking_items.booking_id
    WHERE bookings.note LIKE 'V8_DEMO_BOOKING_%'
    GROUP BY booking_items.shop_id, booking_items.booking_id
) AS booking_totals
WHERE invoices.shop_id = booking_totals.shop_id
  AND invoices.booking_id = booking_totals.booking_id;

INSERT INTO orders (
    shop_id,
    customer_id,
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
    note
)
SELECT
    seed_orders.shop_id,
    customers.id,
    seed_orders.order_code,
    seed_orders.status::order_status,
    seed_orders.source::order_source,
    0,
    seed_orders.shipping_fee,
    seed_orders.discount_amount,
    0,
    customers.full_name,
    customers.phone,
    seed_orders.shipping_address,
    seed_orders.note
FROM (
    VALUES
        (1, '096778899', 'ORD-001', 'PENDING', 'ONLINE', 20000, 0, '12 Nguyen Trai, Quan 1', 'V11_DEMO_ORDER_001'),
        (1, '0912345678', 'ORD-002', 'CONFIRMED', 'ONLINE', 20000, 10000, '88 Le Loi, Quan 3', 'V11_DEMO_ORDER_002'),
        (1, '0987654321', 'ORD-003', 'PACKING', 'ONLINE', 25000, 0, '42 Cach Mang Thang 8, Quan 10', 'V11_DEMO_ORDER_003'),
        (1, '0944556677', 'ORD-004', 'SHIPPING', 'ONLINE', 25000, 0, '7 Phan Xich Long, Phu Nhuan', 'V11_DEMO_ORDER_004'),
        (1, '0901122334', 'ORD-005', 'COMPLETED', 'STAFF', 0, 0, '15 Nguyen Van Linh, Quan 7', 'V11_DEMO_ORDER_005'),
        (1, '0933445566', 'ORD-006', 'CANCELLED', 'ONLINE', 20000, 0, '29 Hoang Dieu, Quan 4', 'V11_DEMO_ORDER_006'),
        (1, '0955667788', 'ORD-007', 'PENDING', 'ONLINE', 20000, 0, '5 Dien Bien Phu, Binh Thanh', 'V11_DEMO_ORDER_007'),
        (1, '096778899', 'ORD-008', 'COMPLETED', 'ONLINE', 0, 5000, '120 Vo Van Tan, Quan 3', 'V11_DEMO_ORDER_008')
) AS seed_orders(shop_id, customer_phone, order_code, status, source, shipping_fee, discount_amount, shipping_address, note)
JOIN customers
    ON customers.shop_id = seed_orders.shop_id
    AND customers.phone = seed_orders.customer_phone
WHERE NOT EXISTS (
    SELECT 1
    FROM orders
    WHERE orders.shop_id = seed_orders.shop_id
      AND orders.order_code = seed_orders.order_code
);

INSERT INTO order_items (shop_id, order_id, product_id, qty, unit_price, amount)
SELECT
    orders.shop_id,
    orders.id,
    products.id,
    seed_items.qty,
    products.price,
    products.price * seed_items.qty
FROM (
    VALUES
        ('ORD-001', 'V8-SUA-TAM-THAO-MOC', 1),
        ('ORD-001', 'V8-HAT-DINH-DUONG-PREMIUM', 1),
        ('ORD-002', 'V8-HAT-DINH-DUONG-PREMIUM', 2),
        ('ORD-003', 'V8-DO-CHOI-GAM-RONG', 3),
        ('ORD-003', 'V8-DUNG-DICH-SAT-KHUAN', 1),
        ('ORD-004', 'V8-SUA-TAM-THAO-MOC', 2),
        ('ORD-005', 'V8-HAT-DINH-DUONG-PREMIUM', 1),
        ('ORD-005', 'V8-DO-CHOI-GAM-RONG', 2),
        ('ORD-006', 'V8-DUNG-DICH-SAT-KHUAN', 1),
        ('ORD-007', 'V8-SUA-TAM-THAO-MOC', 1),
        ('ORD-007', 'V8-DO-CHOI-GAM-RONG', 1),
        ('ORD-008', 'V8-HAT-DINH-DUONG-PREMIUM', 3)
) AS seed_items(order_code, product_sku, qty)
JOIN orders
    ON orders.order_code = seed_items.order_code
    AND orders.note LIKE 'V11_DEMO_ORDER_%'
JOIN products
    ON products.shop_id = orders.shop_id
    AND products.sku = seed_items.product_sku
WHERE NOT EXISTS (
    SELECT 1
    FROM order_items
    WHERE order_items.order_id = orders.id
      AND order_items.product_id = products.id
);

UPDATE orders
SET
    subtotal_amount = order_totals.subtotal_amount,
    total_amount = order_totals.subtotal_amount + orders.shipping_fee - orders.discount_amount
FROM (
    SELECT
        order_items.shop_id,
        order_items.order_id,
        COALESCE(SUM(order_items.amount), 0) AS subtotal_amount
    FROM order_items
    JOIN orders
        ON orders.shop_id = order_items.shop_id
        AND orders.id = order_items.order_id
    WHERE orders.note LIKE 'V11_DEMO_ORDER_%'
    GROUP BY order_items.shop_id, order_items.order_id
) AS order_totals
WHERE orders.shop_id = order_totals.shop_id
  AND orders.id = order_totals.order_id;

INSERT INTO order_status_events (shop_id, order_id, from_status, to_status)
SELECT
    orders.shop_id,
    orders.id,
    NULL,
    orders.status
FROM orders
WHERE orders.note LIKE 'V11_DEMO_ORDER_%'
  AND NOT EXISTS (
      SELECT 1
      FROM order_status_events
      WHERE order_status_events.shop_id = orders.shop_id
        AND order_status_events.order_id = orders.id
  );

INSERT INTO invoices (shop_id, customer_id, order_id, total_amount, status, issued_at)
SELECT
    orders.shop_id,
    orders.customer_id,
    orders.id,
    orders.total_amount,
    CASE
        WHEN orders.status = 'COMPLETED'::order_status THEN 'PAID'::invoice_status
        WHEN orders.status = 'CANCELLED'::order_status THEN 'CANCELLED'::invoice_status
        ELSE 'ISSUED'::invoice_status
    END,
    orders.created_at
FROM orders
WHERE orders.note LIKE 'V11_DEMO_ORDER_%'
  AND NOT EXISTS (
      SELECT 1
      FROM invoices
      WHERE invoices.shop_id = orders.shop_id
        AND invoices.order_id = orders.id
  );
