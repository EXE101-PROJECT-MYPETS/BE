ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS user_id bigint REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS user_id bigint REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE invoices
    ADD COLUMN IF NOT EXISTS user_id bigint REFERENCES users(id) ON DELETE SET NULL;

UPDATE orders
SET user_id = customer.user_id
FROM customers customer
WHERE orders.user_id IS NULL
  AND orders.shop_id = customer.shop_id
  AND orders.customer_id = customer.id
  AND customer.user_id IS NOT NULL;

UPDATE bookings
SET user_id = customer.user_id
FROM customers customer
WHERE bookings.user_id IS NULL
  AND bookings.shop_id = customer.shop_id
  AND bookings.customer_id = customer.id
  AND customer.user_id IS NOT NULL;

UPDATE invoices
SET user_id = customer.user_id
FROM customers customer
WHERE invoices.user_id IS NULL
  AND invoices.shop_id = customer.shop_id
  AND invoices.customer_id = customer.id
  AND customer.user_id IS NOT NULL;

ALTER TABLE bookings
    ALTER COLUMN customer_id DROP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_customers_shop_user
    ON customers(shop_id, user_id)
    WHERE user_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_orders_shop_user_created
    ON orders(shop_id, user_id, created_at);

CREATE INDEX IF NOT EXISTS idx_bookings_shop_user_start
    ON bookings(shop_id, user_id, start_at);

CREATE INDEX IF NOT EXISTS idx_invoices_shop_user_created
    ON invoices(shop_id, user_id, created_at);
