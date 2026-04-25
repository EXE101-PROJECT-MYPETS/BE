DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'order_status') THEN
    CREATE TYPE order_status AS ENUM (
      'PENDING',
      'CONFIRMED',
      'PACKING',
      'SHIPPING',
      'COMPLETED',
      'CANCELLED'
    );
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'order_source') THEN
    CREATE TYPE order_source AS ENUM ('ONLINE', 'STAFF');
  END IF;
END $$;

CREATE TABLE IF NOT EXISTS orders (
  id bigserial PRIMARY KEY,
  shop_id bigint NOT NULL REFERENCES shops(id) ON DELETE CASCADE,

  customer_id bigint NOT NULL,
  CONSTRAINT fk_orders_customer_shop
    FOREIGN KEY (shop_id, customer_id)
    REFERENCES customers(shop_id, id)
    ON DELETE RESTRICT,

  order_code varchar(30),
  status order_status NOT NULL DEFAULT 'PENDING',
  source order_source NOT NULL DEFAULT 'ONLINE',

  subtotal_amount bigint NOT NULL DEFAULT 0 CHECK (subtotal_amount >= 0),
  shipping_fee bigint NOT NULL DEFAULT 0 CHECK (shipping_fee >= 0),
  discount_amount bigint NOT NULL DEFAULT 0 CHECK (discount_amount >= 0),
  total_amount bigint NOT NULL DEFAULT 0 CHECK (total_amount >= 0),

  receiver_name varchar(255),
  receiver_phone varchar(30),
  shipping_address text,
  note text,

  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),

  UNIQUE (shop_id, id),
  UNIQUE (shop_id, order_code)
);

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_orders_updated_at') THEN
    CREATE TRIGGER trg_orders_updated_at
      BEFORE UPDATE ON orders
      FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_orders_shop_created ON orders(shop_id, created_at);
CREATE INDEX IF NOT EXISTS idx_orders_shop_customer_created ON orders(shop_id, customer_id, created_at);
CREATE INDEX IF NOT EXISTS idx_orders_shop_status_created ON orders(shop_id, status, created_at);

CREATE TABLE IF NOT EXISTS order_items (
  id bigserial PRIMARY KEY,
  shop_id bigint NOT NULL REFERENCES shops(id) ON DELETE CASCADE,

  order_id bigint NOT NULL,
  CONSTRAINT fk_order_items_order_shop
    FOREIGN KEY (shop_id, order_id)
    REFERENCES orders(shop_id, id)
    ON DELETE CASCADE,

  product_id bigint NOT NULL,
  CONSTRAINT fk_order_items_product_shop
    FOREIGN KEY (shop_id, product_id)
    REFERENCES products(shop_id, id)
    ON DELETE RESTRICT,

  qty int NOT NULL DEFAULT 1 CHECK (qty > 0),
  unit_price bigint NOT NULL DEFAULT 0 CHECK (unit_price >= 0),
  amount bigint NOT NULL DEFAULT 0 CHECK (amount >= 0),

  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_shop_order ON order_items(shop_id, order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product_id ON order_items(product_id);

CREATE TABLE IF NOT EXISTS order_status_events (
  id bigserial PRIMARY KEY,
  shop_id bigint NOT NULL REFERENCES shops(id) ON DELETE CASCADE,

  order_id bigint NOT NULL,
  CONSTRAINT fk_order_status_events_order_shop
    FOREIGN KEY (shop_id, order_id)
    REFERENCES orders(shop_id, id)
    ON DELETE CASCADE,

  from_status order_status,
  to_status order_status NOT NULL,

  actor_user_id bigint REFERENCES users(id) ON DELETE SET NULL,
  meta_json jsonb,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_order_status_events_order_created
  ON order_status_events(order_id, created_at);
