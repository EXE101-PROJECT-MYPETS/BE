WITH selected_category AS (
    SELECT 22::bigint AS id
)
INSERT INTO services (shop_id, name, duration_min, base_price, active, category_id, image_url)
SELECT
    seed_services.shop_id,
    seed_services.name,
    seed_services.duration_min,
    seed_services.base_price,
    seed_services.active,
    selected_category.id AS category_id,
    seed_services.image_url
FROM (
    VALUES
        (2, 'Tắm vệ sinh cơ bản', 30, 120000, true, '/uploads/shops/2/services/test/avatar-mac-dinh.jpg'),
        (2, 'Tắm dưỡng lông mềm mượt', 45, 180000, true, '/uploads/shops/2/services/test/avatar-mac-dinh.jpg'),
        (2, 'Cắt tỉa tạo kiểu', 75, 320000, true, '/uploads/shops/2/services/test/avatar-mac-dinh.jpg'),
        (2, 'Cắt móng và vệ sinh chân', 25, 90000, true, '/uploads/shops/2/services/test/avatar-mac-dinh.jpg'),
        (2, 'Vệ sinh tai và khử mùi', 20, 80000, true, '/uploads/shops/2/services/test/avatar-mac-dinh.jpg'),
        (2, 'Chăm sóc da nhạy cảm', 50, 230000, true, '/uploads/shops/2/services/test/avatar-mac-dinh.jpg'),
        (2, 'Spa thư giãn thảo mộc', 60, 260000, true, '/uploads/shops/2/services/test/avatar-mac-dinh.jpg'),
        (2, 'Gói chăm sóc toàn diện', 90, 420000, true, '/uploads/shops/2/services/test/avatar-mac-dinh.jpg')
) AS seed_services(shop_id, name, duration_min, base_price, active, image_url)
CROSS JOIN selected_category
WHERE EXISTS (
    SELECT 1
    FROM shops
    WHERE shops.id = seed_services.shop_id
)
AND EXISTS (
    SELECT 1
    FROM service_categories sc
    WHERE sc.shop_id = seed_services.shop_id
      AND sc.id = selected_category.id
)
ON CONFLICT (shop_id, name) DO UPDATE SET
    duration_min = EXCLUDED.duration_min,
    base_price = EXCLUDED.base_price,
    active = EXCLUDED.active,
    category_id = EXCLUDED.category_id,
    image_url = EXCLUDED.image_url;

SELECT setval(
    pg_get_serial_sequence('services', 'id'),
    COALESCE((SELECT MAX(id) FROM services), 1),
    true
);
