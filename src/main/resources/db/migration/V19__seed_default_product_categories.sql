INSERT INTO product_categories (shop_id, name, description, sort_order)
SELECT
    shops.id,
    categories.name,
    categories.description,
    categories.sort_order
FROM shops
CROSS JOIN (
    VALUES
        ('Thuc an', 'San pham thuc an va dinh duong cho thu cung', 10),
        ('Cham soc', 'San pham tam rua, ve sinh va cham soc co the', 20),
        ('Do choi', 'San pham do choi va giai tri cho thu cung', 30),
        ('Phu kien', 'San pham phu kien, vat dung va do dung di kem', 40)
) AS categories(name, description, sort_order)
ON CONFLICT (shop_id, name) DO NOTHING;

UPDATE products
SET category_id = product_categories.id
FROM product_categories
WHERE products.shop_id = product_categories.shop_id
  AND products.category_id IS NULL
  AND (
      (products.sku = 'V8-HAT-DINH-DUONG-PREMIUM' AND product_categories.name = 'Thuc an')
      OR (products.sku IN ('V8-SUA-TAM-THAO-MOC', 'V8-DUNG-DICH-SAT-KHUAN') AND product_categories.name = 'Cham soc')
      OR (products.sku = 'V8-DO-CHOI-GAM-RONG' AND product_categories.name = 'Do choi')
  );
