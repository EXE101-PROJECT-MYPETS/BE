WITH target_product AS (
    SELECT p.shop_id, p.id AS product_id
    FROM products p
    WHERE p.id = 34
    LIMIT 1
),
seed_customers AS (
    SELECT
        c.id AS customer_id,
        tp.shop_id,
        tp.product_id,
        ROW_NUMBER() OVER (ORDER BY c.created_at ASC, c.id ASC) AS rn
    FROM customers c
    JOIN target_product tp ON tp.shop_id = c.shop_id
    LIMIT 5
),
seed_reviews AS (
    SELECT
        sc.shop_id,
        sc.product_id,
        sc.customer_id,
        CASE sc.rn
            WHEN 1 THEN 5
            WHEN 2 THEN 5
            WHEN 3 THEN 4
            WHEN 4 THEN 4
            ELSE 3
        END AS rating,
        CASE sc.rn
            WHEN 1 THEN 'Sản phẩm rất tốt, đúng mô tả và shop đóng gói cẩn thận.'
            WHEN 2 THEN 'Thú cưng nhà mình dùng hợp, sẽ mua lại.'
            WHEN 3 THEN 'Chất lượng ổn trong tầm giá.'
            WHEN 4 THEN 'Giao hàng nhanh, sản phẩm dùng ổn.'
            ELSE 'Tạm ổn, phù hợp nhu cầu cơ bản.'
        END AS comment,
        now() - (sc.rn * interval '2 days') AS created_at
    FROM seed_customers sc
)
INSERT INTO reviews (
    shop_id,
    product_id,
    customer_id,
    rating,
    comment,
    created_at
)
SELECT
    sr.shop_id,
    sr.product_id,
    sr.customer_id,
    sr.rating,
    sr.comment,
    sr.created_at
FROM seed_reviews sr
ON CONFLICT (shop_id, product_id, customer_id) DO UPDATE SET
    rating = EXCLUDED.rating,
    comment = EXCLUDED.comment,
    updated_at = now();
