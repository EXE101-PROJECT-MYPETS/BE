WITH target_services AS (
    SELECT
        s.shop_id,
        s.id AS service_id,
        s.name AS service_name
    FROM prod.services s
    WHERE s.id IN (65, 71)
),
target_shops AS (
    SELECT DISTINCT shop_id
    FROM target_services
),
seed_customers AS (
    SELECT
        ts.shop_id,
        sc.full_name,
        sc.phone,
        sc.email
    FROM target_shops ts
    CROSS JOIN (
        VALUES
            ('Nguyễn Hoàng Minh', '0906500071', 'nguyen.hoang.minh.service.review@example.com'),
            ('Trần Bảo Ngọc', '0906500072', 'tran.bao.ngoc.service.review@example.com'),
            ('Lê Gia Huy', '0906500073', 'le.gia.huy.service.review@example.com')
    ) AS sc(full_name, phone, email)
),
upsert_customers AS (
    INSERT INTO prod.customers (shop_id, full_name, phone, email)
    SELECT
        shop_id,
        full_name,
        phone,
        email
    FROM seed_customers
    ON CONFLICT (shop_id, phone) WHERE phone IS NOT NULL DO UPDATE SET
        full_name = EXCLUDED.full_name,
        email = EXCLUDED.email
    RETURNING shop_id, id, phone
),
target_customers AS (
    SELECT
        c.shop_id,
        c.id AS customer_id,
        c.phone,
        ROW_NUMBER() OVER (
            PARTITION BY c.shop_id
            ORDER BY
                CASE c.phone
                    WHEN '0906500071' THEN 1
                    WHEN '0906500072' THEN 2
                    WHEN '0906500073' THEN 3
                    ELSE 99
                END,
                c.id
        ) AS customer_slot
    FROM prod.customers c
    JOIN target_shops ts
        ON ts.shop_id = c.shop_id
    WHERE c.phone IN ('0906500071', '0906500072', '0906500073')
),
seed_reviews AS (
    SELECT
        ts.shop_id,
        ts.service_id,
        tc.customer_id,
        review_seed.rating,
        review_seed.comment,
        now() - (review_seed.customer_slot * interval '1 day') AS created_at
    FROM target_services ts
    JOIN (
        VALUES
            (65::bigint, 1, 5, 'Dịch vụ rất tốt, nhân viên tư vấn kỹ và thao tác cẩn thận.'),
            (65::bigint, 2, 4, 'Trải nghiệm ổn, thú cưng được chăm sóc nhẹ nhàng và sạch sẽ.'),
            (65::bigint, 3, 5, 'Rất hài lòng với chất lượng dịch vụ, sẽ quay lại lần sau.'),
            (71::bigint, 1, 5, 'Bác sĩ kiểm tra kỹ, giải thích rõ tình trạng của thú cưng.'),
            (71::bigint, 2, 4, 'Dịch vụ tốt, quy trình nhanh gọn và thái độ phục vụ ổn.'),
            (71::bigint, 3, 5, 'Shop chăm sóc rất chuyên nghiệp, mình hoàn toàn yên tâm.')
    ) AS review_seed(service_id, customer_slot, rating, comment)
        ON review_seed.service_id = ts.service_id
    JOIN target_customers tc
        ON tc.shop_id = ts.shop_id
       AND tc.customer_slot = review_seed.customer_slot
)
INSERT INTO prod.service_reviews (
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
