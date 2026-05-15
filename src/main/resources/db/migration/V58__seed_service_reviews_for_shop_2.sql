INSERT INTO customers (shop_id, full_name, phone, email)
SELECT
    seed_customers.shop_id,
    seed_customers.full_name,
    seed_customers.phone,
    seed_customers.email
FROM (
    VALUES
        (2, 'Nguyễn Minh Anh', '0902200001', 'nguyen.minh.anh.shop2@example.com'),
        (2, 'Trần Gia Hân', '0902200002', 'tran.gia.han.shop2@example.com'),
        (2, 'Lê Quốc Bảo', '0902200003', 'le.quoc.bao.shop2@example.com')
) AS seed_customers(shop_id, full_name, phone, email)
WHERE EXISTS (
    SELECT 1
    FROM shops
    WHERE shops.id = seed_customers.shop_id
)
ON CONFLICT (shop_id, phone) WHERE phone IS NOT NULL DO UPDATE SET
    full_name = EXCLUDED.full_name,
    email = EXCLUDED.email;

WITH target_services AS (
    SELECT
        s.shop_id,
        s.id AS service_id,
        s.name AS service_name,
        ROW_NUMBER() OVER (ORDER BY s.id ASC) AS service_rn
    FROM services s
    WHERE s.shop_id = 2
),
target_customers AS (
    SELECT
        c.shop_id,
        c.id AS customer_id,
        ROW_NUMBER() OVER (ORDER BY c.created_at ASC, c.id ASC) AS customer_rn
    FROM customers c
    WHERE c.shop_id = 2
    LIMIT 3
),
seed_reviews AS (
    SELECT
        ts.shop_id,
        ts.service_id,
        tc.customer_id,
        CASE tc.customer_rn
            WHEN 1 THEN 5
            WHEN 2 THEN 4
            ELSE 5
        END AS rating,
        CASE tc.customer_rn
            WHEN 1 THEN CONCAT('Dịch vụ ', ts.service_name, ' rất tốt, nhân viên chăm sóc kỹ.')
            WHEN 2 THEN CONCAT('Trải nghiệm ', ts.service_name, ' ổn, thú cưng hợp tác tốt.')
            ELSE CONCAT('Hài lòng với ', ts.service_name, ', sẽ quay lại lần sau.')
        END AS comment,
        now() - ((ts.service_rn + tc.customer_rn) * interval '1 day') AS created_at
    FROM target_services ts
    JOIN target_customers tc
        ON tc.shop_id = ts.shop_id
)
INSERT INTO service_reviews (
    shop_id,
    service_id,
    customer_id,
    rating,
    comment,
    created_at
)
SELECT
    sr.shop_id,
    sr.service_id,
    sr.customer_id,
    sr.rating,
    sr.comment,
    sr.created_at
FROM seed_reviews sr
ON CONFLICT (shop_id, service_id, customer_id) DO UPDATE SET
    rating = EXCLUDED.rating,
    comment = EXCLUDED.comment,
    updated_at = now();
