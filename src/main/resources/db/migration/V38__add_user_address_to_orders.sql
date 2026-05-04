ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS user_address_id bigint REFERENCES user_addresses(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_orders_user_address_id
    ON orders(user_address_id);

UPDATE orders
SET user_address_id = default_addresses.id
FROM customers
JOIN user_addresses default_addresses
    ON default_addresses.user_id = customers.user_id
    AND default_addresses.is_default = true
WHERE orders.customer_id = customers.id
  AND orders.shop_id = customers.shop_id
  AND orders.user_address_id IS NULL;
