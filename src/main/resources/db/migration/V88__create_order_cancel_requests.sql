DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'order_cancel_request_status') THEN
    CREATE TYPE order_cancel_request_status AS ENUM (
      'PENDING',
      'APPROVED',
      'REJECTED'
    );
  END IF;
END $$;

CREATE TABLE IF NOT EXISTS order_cancel_requests (
  id bigserial PRIMARY KEY,
  shop_id bigint NOT NULL REFERENCES shops(id) ON DELETE CASCADE,

  order_id bigint NOT NULL,
  CONSTRAINT fk_order_cancel_requests_order_shop
    FOREIGN KEY (shop_id, order_id)
    REFERENCES orders(shop_id, id)
    ON DELETE CASCADE,

  user_id bigint REFERENCES users(id) ON DELETE SET NULL,
  reason text,
  status order_cancel_request_status NOT NULL DEFAULT 'PENDING',
  reviewed_by bigint REFERENCES users(id) ON DELETE SET NULL,
  review_note text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_order_cancel_requests_updated_at') THEN
    CREATE TRIGGER trg_order_cancel_requests_updated_at
      BEFORE UPDATE ON order_cancel_requests
      FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_order_cancel_requests_shop_status_created
  ON order_cancel_requests(shop_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_order_cancel_requests_order_created
  ON order_cancel_requests(order_id, created_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS uq_order_cancel_requests_order_pending
  ON order_cancel_requests(order_id)
  WHERE status = 'PENDING';
