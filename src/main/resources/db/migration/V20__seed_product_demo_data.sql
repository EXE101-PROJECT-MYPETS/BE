DROP TABLE IF EXISTS seed_product_demo;

CREATE TEMP TABLE seed_product_demo (
    shop_id bigint NOT NULL,
    sku varchar(64) NOT NULL,
    name varchar(255) NOT NULL,
    category_name varchar(100) NOT NULL,
    unit varchar(50),
    price bigint NOT NULL,
    active boolean NOT NULL,
    stock_qty bigint NOT NULL,
    image_url_1 text,
    image_url_2 text
) ON COMMIT DROP;

INSERT INTO seed_product_demo (
    shop_id,
    sku,
    name,
    category_name,
    unit,
    price,
    active,
    stock_qty,
    image_url_1,
    image_url_2
)
VALUES
    (1, 'V8-SUA-TAM-THAO-MOC', 'Sua tam thao moc', 'Cham soc', 'chai', 120000, true, 24, 'shops/1/products/demo/v8-sua-tam-thao-moc-1.jpg', 'shops/1/products/demo/v8-sua-tam-thao-moc-2.jpg'),
    (1, 'V8-HAT-DINH-DUONG-PREMIUM', 'Hat dinh duong premium', 'Thuc an', 'goi', 129000, true, 40, 'shops/1/products/demo/v8-hat-dinh-duong-premium-1.jpg', 'shops/1/products/demo/v8-hat-dinh-duong-premium-2.jpg'),
    (1, 'V8-DO-CHOI-GAM-RONG', 'Do choi gam rong', 'Do choi', 'cai', 59000, true, 18, 'shops/1/products/demo/v8-do-choi-gam-rong-1.jpg', 'shops/1/products/demo/v8-do-choi-gam-rong-2.jpg'),
    (1, 'V8-DUNG-DICH-SAT-KHUAN', 'Dung dich sat khuan', 'Cham soc', 'chai', 99000, true, 15, 'shops/1/products/demo/v8-dung-dich-sat-khuan-1.jpg', NULL),
    (1, 'V20-PATE-GA-MEM', 'Pate ga mem', 'Thuc an', 'hop', 35000, true, 60, 'shops/1/products/demo/v20-pate-ga-mem-1.jpg', NULL),
    (1, 'V20-BANH-THUONG-PHOMAI', 'Banh thuong pho mai', 'Thuc an', 'tui', 45000, true, 55, 'shops/1/products/demo/v20-banh-thuong-pho-mai-1.jpg', NULL),
    (1, 'V20-HAT-CHO-MEO-INDOOR', 'Hat cho meo indoor', 'Thuc an', 'goi', 165000, true, 28, 'shops/1/products/demo/v20-hat-cho-meo-indoor-1.jpg', NULL),
    (1, 'V20-SUA-BO-SUNG-CANXI', 'Sua bo sung canxi', 'Thuc an', 'hop', 89000, true, 22, 'shops/1/products/demo/v20-sua-bo-sung-canxi-1.jpg', NULL),
    (1, 'V20-XIT-KHU-MUI-TOILET', 'Xit khu mui toilet', 'Cham soc', 'chai', 78000, true, 19, 'shops/1/products/demo/v20-xit-khu-mui-toilet-1.jpg', NULL),
    (1, 'V20-KHAN-UOT-CHAM-SOC', 'Khan uot cham soc', 'Cham soc', 'goi', 32000, true, 48, 'shops/1/products/demo/v20-khan-uot-cham-soc-1.jpg', NULL),
    (1, 'V20-BOT-TAM-KHO', 'Bot tam kho', 'Cham soc', 'chai', 110000, true, 14, 'shops/1/products/demo/v20-bot-tam-kho-1.jpg', NULL),
    (1, 'V20-LUOC-CHAI-LONG', 'Luoc chai long', 'Phu kien', 'cai', 69000, true, 30, 'shops/1/products/demo/v20-luoc-chai-long-1.jpg', NULL),
    (1, 'V20-BAT-INOX-CHONG-TRUOT', 'Bat inox chong truot', 'Phu kien', 'cai', 85000, true, 26, 'shops/1/products/demo/v20-bat-inox-chong-truot-1.jpg', NULL),
    (1, 'V20-DAY-DAT-PHAN-QUANG', 'Day dat phan quang', 'Phu kien', 'cai', 125000, true, 16, 'shops/1/products/demo/v20-day-dat-phan-quang-1.jpg', NULL),
    (1, 'V20-VONG-CO-DA-MEM', 'Vong co da mem', 'Phu kien', 'cai', 99000, true, 21, 'shops/1/products/demo/v20-vong-co-da-mem-1.jpg', NULL),
    (1, 'V20-TUI-XACH-THU-CUNG', 'Tui xach thu cung', 'Phu kien', 'cai', 265000, false, 8, 'shops/1/products/demo/v20-tui-xach-thu-cung-1.jpg', NULL),
    (1, 'V20-DO-CHOI-BONG-KEU', 'Do choi bong keu', 'Do choi', 'cai', 49000, true, 33, 'shops/1/products/demo/v20-do-choi-bong-keu-1.jpg', NULL),
    (1, 'V20-DO-CHOI-DAY-THUNG', 'Do choi day thung', 'Do choi', 'cai', 57000, true, 29, 'shops/1/products/demo/v20-do-choi-day-thung-1.jpg', NULL),
    (1, 'V20-CAN-CU-SO-VE-SINH', 'Can cu so ve sinh', 'Phu kien', 'cai', 210000, true, 10, 'shops/1/products/demo/v20-can-cu-so-ve-sinh-1.jpg', NULL),
    (1, 'V20-NHAI-GAM-CAO-SU', 'Nhai gam cao su', 'Do choi', 'cai', 39000, true, 36, 'shops/1/products/demo/v20-nhai-gam-cao-su-1.jpg', NULL);

INSERT INTO products (
    shop_id,
    sku,
    name,
    category_id,
    unit,
    price,
    active
)
SELECT
    seed.shop_id,
    seed.sku,
    seed.name,
    product_categories.id,
    seed.unit,
    seed.price,
    seed.active
FROM seed_product_demo seed
JOIN shops
    ON shops.id = seed.shop_id
JOIN product_categories
    ON product_categories.shop_id = seed.shop_id
    AND product_categories.name = seed.category_name
ON CONFLICT (shop_id, sku) DO UPDATE SET
    name = EXCLUDED.name,
    category_id = EXCLUDED.category_id,
    unit = EXCLUDED.unit,
    price = EXCLUDED.price,
    active = EXCLUDED.active;

INSERT INTO inventory (
    shop_id,
    product_id,
    on_hand,
    reserved,
    updated_at
)
SELECT
    seed.shop_id,
    products.id,
    seed.stock_qty,
    0,
    now()
FROM seed_product_demo seed
JOIN products
    ON products.shop_id = seed.shop_id
    AND products.sku = seed.sku
ON CONFLICT (shop_id, product_id) DO UPDATE SET
    on_hand = GREATEST(inventory.on_hand, inventory.reserved, EXCLUDED.on_hand),
    reserved = LEAST(inventory.reserved, GREATEST(inventory.on_hand, inventory.reserved, EXCLUDED.on_hand)),
    updated_at = now();

INSERT INTO product_images (
    shop_id,
    product_id,
    image_url,
    sort_order
)
SELECT
    seed.shop_id,
    products.id,
    image_data.image_url,
    image_data.sort_order
FROM seed_product_demo seed
JOIN products
    ON products.shop_id = seed.shop_id
    AND products.sku = seed.sku
CROSS JOIN LATERAL (
    VALUES
        (0, seed.image_url_1),
        (1, seed.image_url_2)
) AS image_data(sort_order, image_url)
WHERE image_data.image_url IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM product_images
      WHERE product_images.shop_id = seed.shop_id
        AND product_images.product_id = products.id
        AND product_images.sort_order = image_data.sort_order
  );
