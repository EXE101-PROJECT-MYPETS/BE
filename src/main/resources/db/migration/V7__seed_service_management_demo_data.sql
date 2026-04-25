INSERT INTO services (shop_id, name, duration_min, base_price, active, category_id)
SELECT
    seed_services.shop_id,
    seed_services.name,
    seed_services.duration_min,
    seed_services.base_price,
    seed_services.active,
    seed_services.category_id
FROM (
    VALUES
        (1, 'Tam co ban', 30, 120000, true, 1),
        (1, 'Tam duong long', 45, 180000, true, 1),
        (1, 'Tam khu mui', 40, 150000, true, 1),
        (1, 'Tam trang long', 60, 220000, true, 1),
        (1, 'Tam spa nhe', 50, 200000, true, 1),
        (1, 'Tam cham soc da', 55, 210000, true, 1),
        (1, 'Tam thu cung cao cap', 70, 300000, true, 1),
        (1, 'Tam nhanh', 25, 100000, true, 1),
        (1, 'Tam ket hop massage', 65, 270000, true, 1),
        (1, 'Tam duong am', 45, 190000, false, 1),
        (1, 'Cat tia co ban', 60, 250000, true, 7),
        (1, 'Cat tia tao kieu', 90, 350000, true, 7),
        (1, 'Cao long mua he', 75, 280000, true, 7),
        (1, 'Chai long roi', 45, 170000, true, 7),
        (1, 'Cat tia nhanh', 40, 200000, true, 7),
        (1, 'Tao kieu cao cap', 100, 400000, true, 7),
        (1, 'Cat long theo yeu cau', 80, 320000, true, 7),
        (1, 'Cat long nghe thuat', 110, 450000, false, 7),
        (1, 'Grooming toan dien', 120, 500000, true, 7),
        (1, 'Cham soc long chuyen sau', 90, 380000, true, 7)
) AS seed_services(shop_id, name, duration_min, base_price, active, category_id)
WHERE EXISTS (
    SELECT 1
    FROM shops
    WHERE shops.id = seed_services.shop_id
)
AND EXISTS (
    SELECT 1
    FROM service_categories
    WHERE service_categories.shop_id = seed_services.shop_id
      AND service_categories.id = seed_services.category_id
)
ON CONFLICT (shop_id, name) DO UPDATE SET
    duration_min = EXCLUDED.duration_min,
    base_price = EXCLUDED.base_price,
    active = EXCLUDED.active,
    category_id = EXCLUDED.category_id;

SELECT setval(
    pg_get_serial_sequence('services', 'id'),
    COALESCE((SELECT MAX(id) FROM services), 1),
    true
);
