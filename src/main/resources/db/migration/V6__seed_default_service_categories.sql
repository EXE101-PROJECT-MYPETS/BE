INSERT INTO service_categories (shop_id, name, description, sort_order)
SELECT
    shops.id,
    categories.name,
    categories.description,
    categories.sort_order
FROM shops
CROSS JOIN (
    VALUES
        ('Tắm và vệ sinh', 'Dịch vụ tắm rửa và vệ sinh cơ bản', 10),
        ('Cắt tỉa lông', 'Dịch vụ cắt tỉa, tạo kiểu và chăm sóc lông', 20),
        ('Chăm sóc móng', 'Dịch vụ cắt móng và chăm sóc bàn chân', 30),
        ('Chăm sóc sức khỏe', 'Dịch vụ chăm sóc sức khỏe cơ bản', 40),
        ('Spa thư giãn', 'Dịch vụ spa và thư giãn cho thú cưng', 50)
) AS categories(name, description, sort_order)
ON CONFLICT (shop_id, name) DO NOTHING;
