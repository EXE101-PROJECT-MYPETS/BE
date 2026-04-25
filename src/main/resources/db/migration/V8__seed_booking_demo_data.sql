INSERT INTO customers (shop_id, full_name, phone, email)
SELECT
    seed_customers.shop_id,
    seed_customers.full_name,
    seed_customers.phone,
    seed_customers.email
FROM (
    VALUES
        (1, 'Do Thi Thanh', '096778899', 'do.thi.thanh@example.com'),
        (1, 'Hoang Minh Tuan', '0912345678', 'hoang.minh.tuan@example.com'),
        (1, 'Nguyen Thi Lan', '0987654321', 'nguyen.thi.lan@example.com'),
        (1, 'Le Duc Thinh', '0944556677', 'le.duc.thinh@example.com'),
        (1, 'Tran Van Khoa', '0901122334', 'tran.van.khoa@example.com'),
        (1, 'Pham Thu Ha', '0933445566', 'pham.thu.ha@example.com'),
        (1, 'Vo Minh Nhat', '0955667788', 'vo.minh.nhat@example.com')
) AS seed_customers(shop_id, full_name, phone, email)
WHERE EXISTS (
    SELECT 1
    FROM shops
    WHERE shops.id = seed_customers.shop_id
)
ON CONFLICT (shop_id, phone) WHERE phone IS NOT NULL DO UPDATE SET
    full_name = EXCLUDED.full_name,
    email = EXCLUDED.email;

INSERT INTO products (shop_id, sku, name, unit, price, active)
SELECT
    seed_products.shop_id,
    seed_products.sku,
    seed_products.name,
    seed_products.unit,
    seed_products.price,
    true
FROM (
    VALUES
        (1, 'V8-SUA-TAM-THAO-MOC', 'Sua tam thao moc', 'chai', 120000),
        (1, 'V8-HAT-DINH-DUONG-PREMIUM', 'Hat dinh duong premium', 'goi', 129000),
        (1, 'V8-DO-CHOI-GAM-RONG', 'Do choi gam rong', 'cai', 59000),
        (1, 'V8-DUNG-DICH-SAT-KHUAN', 'Dung dich sat khuan', 'chai', 99000)
) AS seed_products(shop_id, sku, name, unit, price)
WHERE EXISTS (
    SELECT 1
    FROM shops
    WHERE shops.id = seed_products.shop_id
)
ON CONFLICT (shop_id, sku) DO UPDATE SET
    name = EXCLUDED.name,
    unit = EXCLUDED.unit,
    price = EXCLUDED.price,
    active = EXCLUDED.active;

INSERT INTO bookings (shop_id, customer_id, start_at, end_at, status, source, note, created_by)
SELECT
    seed_bookings.shop_id,
    customers.id,
    seed_bookings.start_at,
    seed_bookings.end_at,
    seed_bookings.status::booking_status,
    seed_bookings.source::booking_source,
    seed_bookings.note,
    CASE
        WHEN seed_bookings.created_by IS NOT NULL
             AND EXISTS (SELECT 1 FROM users WHERE users.id = seed_bookings.created_by)
            THEN seed_bookings.created_by
        ELSE NULL
    END
FROM (
    VALUES
        (1, '096778899', '2026-03-17 09:15:00+07'::timestamptz, '2026-03-17 10:15:00+07'::timestamptz, 'DRAFT', 'CUSTOMER', 'V8_DEMO_BOOKING_001', NULL),
        (1, '0912345678', '2026-03-17 08:30:00+07'::timestamptz, '2026-03-17 09:30:00+07'::timestamptz, 'DRAFT', 'CUSTOMER', 'V8_DEMO_BOOKING_002', NULL),
        (1, '0987654321', '2026-03-17 07:10:00+07'::timestamptz, '2026-03-17 08:10:00+07'::timestamptz, 'CONFIRMED', 'STAFF', 'V8_DEMO_BOOKING_003', 1),
        (1, '0944556677', '2026-03-17 06:00:00+07'::timestamptz, '2026-03-17 07:00:00+07'::timestamptz, 'IN_PROGRESS', 'STAFF', 'V8_DEMO_BOOKING_004', 1),
        (1, '0901122334', '2026-03-16 15:45:00+07'::timestamptz, '2026-03-16 16:45:00+07'::timestamptz, 'COMPLETED', 'STAFF', 'V8_DEMO_BOOKING_005', 1),
        (1, '0933445566', '2026-03-16 10:20:00+07'::timestamptz, '2026-03-16 11:20:00+07'::timestamptz, 'COMPLETED', 'STAFF', 'V8_DEMO_BOOKING_006', 1),
        (1, '0955667788', '2026-03-15 14:00:00+07'::timestamptz, '2026-03-15 15:00:00+07'::timestamptz, 'CANCELLED', 'CUSTOMER', 'V8_DEMO_BOOKING_007', NULL),
        (1, '096778899', '2026-03-15 11:30:00+07'::timestamptz, '2026-03-15 12:30:00+07'::timestamptz, 'CONFIRMED', 'STAFF', 'V8_DEMO_BOOKING_008', 1),
        (1, '0912345678', '2026-03-15 09:45:00+07'::timestamptz, '2026-03-15 10:45:00+07'::timestamptz, 'IN_PROGRESS', 'STAFF', 'V8_DEMO_BOOKING_009', 1),
        (1, '0987654321', '2026-03-14 16:20:00+07'::timestamptz, '2026-03-14 17:20:00+07'::timestamptz, 'DRAFT', 'CUSTOMER', 'V8_DEMO_BOOKING_010', NULL),
        (1, '0944556677', '2026-03-14 13:10:00+07'::timestamptz, '2026-03-14 14:10:00+07'::timestamptz, 'CONFIRMED', 'STAFF', 'V8_DEMO_BOOKING_011', 1),
        (1, '0901122334', '2026-03-14 10:05:00+07'::timestamptz, '2026-03-14 11:05:00+07'::timestamptz, 'COMPLETED', 'STAFF', 'V8_DEMO_BOOKING_012', 1),
        (1, '0933445566', '2026-03-13 17:00:00+07'::timestamptz, '2026-03-13 18:00:00+07'::timestamptz, 'NO_SHOW', 'CUSTOMER', 'V8_DEMO_BOOKING_013', NULL),
        (1, '0955667788', '2026-03-13 15:15:00+07'::timestamptz, '2026-03-13 16:15:00+07'::timestamptz, 'DRAFT', 'CUSTOMER', 'V8_DEMO_BOOKING_014', NULL),
        (1, '096778899', '2026-03-13 08:40:00+07'::timestamptz, '2026-03-13 09:40:00+07'::timestamptz, 'CONFIRMED', 'STAFF', 'V8_DEMO_BOOKING_015', 1),
        (1, '0912345678', '2026-03-12 18:10:00+07'::timestamptz, '2026-03-12 19:10:00+07'::timestamptz, 'IN_PROGRESS', 'STAFF', 'V8_DEMO_BOOKING_016', 1),
        (1, '0987654321', '2026-03-12 14:25:00+07'::timestamptz, '2026-03-12 15:25:00+07'::timestamptz, 'COMPLETED', 'STAFF', 'V8_DEMO_BOOKING_017', 1),
        (1, '0944556677', '2026-03-12 09:00:00+07'::timestamptz, '2026-03-12 10:00:00+07'::timestamptz, 'DRAFT', 'CUSTOMER', 'V8_DEMO_BOOKING_018', NULL),
        (1, '0901122334', '2026-03-11 16:35:00+07'::timestamptz, '2026-03-11 17:35:00+07'::timestamptz, 'CONFIRMED', 'STAFF', 'V8_DEMO_BOOKING_019', 1),
        (1, '0933445566', '2026-03-11 12:50:00+07'::timestamptz, '2026-03-11 13:50:00+07'::timestamptz, 'CANCELLED', 'CUSTOMER', 'V8_DEMO_BOOKING_020', NULL),
        (1, '0955667788', '2026-03-11 08:15:00+07'::timestamptz, '2026-03-11 09:15:00+07'::timestamptz, 'COMPLETED', 'STAFF', 'V8_DEMO_BOOKING_021', 1),
        (1, '096778899', '2026-03-10 17:30:00+07'::timestamptz, '2026-03-10 18:30:00+07'::timestamptz, 'DRAFT', 'CUSTOMER', 'V8_DEMO_BOOKING_022', NULL),
        (1, '0912345678', '2026-03-10 13:40:00+07'::timestamptz, '2026-03-10 14:40:00+07'::timestamptz, 'CONFIRMED', 'STAFF', 'V8_DEMO_BOOKING_023', 1),
        (1, '0987654321', '2026-03-10 10:55:00+07'::timestamptz, '2026-03-10 11:55:00+07'::timestamptz, 'IN_PROGRESS', 'STAFF', 'V8_DEMO_BOOKING_024', 1),
        (1, '0944556677', '2026-03-09 09:20:00+07'::timestamptz, '2026-03-09 10:20:00+07'::timestamptz, 'COMPLETED', 'STAFF', 'V8_DEMO_BOOKING_025', 1)
) AS seed_bookings(shop_id, customer_phone, start_at, end_at, status, source, note, created_by)
JOIN customers
    ON customers.shop_id = seed_bookings.shop_id
    AND customers.phone = seed_bookings.customer_phone
WHERE NOT EXISTS (
    SELECT 1
    FROM bookings
    WHERE bookings.shop_id = seed_bookings.shop_id
      AND bookings.note = seed_bookings.note
);

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

INSERT INTO invoices (shop_id, customer_id, booking_id, total_amount, status, issued_at)
SELECT
    bookings.shop_id,
    bookings.customer_id,
    bookings.id,
    COALESCE(SUM(booking_items.amount), 0),
    CASE
        WHEN bookings.status = 'COMPLETED'::booking_status THEN 'PAID'::invoice_status
        WHEN bookings.status IN ('CANCELLED'::booking_status, 'NO_SHOW'::booking_status) THEN 'CANCELLED'::invoice_status
        ELSE 'ISSUED'::invoice_status
    END,
    bookings.start_at
FROM bookings
LEFT JOIN booking_items
    ON booking_items.shop_id = bookings.shop_id
    AND booking_items.booking_id = bookings.id
WHERE bookings.note LIKE 'V8_DEMO_BOOKING_%'
  AND NOT EXISTS (
      SELECT 1
      FROM invoices
      WHERE invoices.shop_id = bookings.shop_id
        AND invoices.booking_id = bookings.id
  )
GROUP BY bookings.shop_id, bookings.customer_id, bookings.id, bookings.status, bookings.start_at;
