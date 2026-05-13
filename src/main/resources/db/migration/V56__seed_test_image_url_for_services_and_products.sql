UPDATE services
SET image_url = '/uploads/shops/1/services/test/86566fa1-5a21-46a2-82b2-f00a3331b853-my-avatar_-_Copy__2_.png';

UPDATE product_images
SET image_url = '/uploads/shops/1/services/test/86566fa1-5a21-46a2-82b2-f00a3331b853-my-avatar_-_Copy__2_.png';

INSERT INTO product_images (shop_id, product_id, image_url, sort_order)
SELECT
    p.shop_id,
    p.id,
    '/uploads/shops/1/services/test/86566fa1-5a21-46a2-82b2-f00a3331b853-my-avatar_-_Copy__2_.png',
    0
FROM products p
WHERE NOT EXISTS (
    SELECT 1
    FROM product_images pi
    WHERE pi.shop_id = p.shop_id
      AND pi.product_id = p.id
);
