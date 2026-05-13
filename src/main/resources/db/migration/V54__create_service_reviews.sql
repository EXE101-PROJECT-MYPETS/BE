CREATE TABLE IF NOT EXISTS service_reviews (
    id bigserial PRIMARY KEY,
    shop_id bigint NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    service_id bigint NOT NULL,
    customer_id bigint NOT NULL,
    rating int NOT NULL,
    comment text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT chk_service_reviews_rating_range CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT uq_service_reviews_shop_service_customer UNIQUE (shop_id, service_id, customer_id),
    CONSTRAINT fk_service_reviews_service_shop
        FOREIGN KEY (shop_id, service_id)
        REFERENCES services(shop_id, id)
        ON DELETE CASCADE,
    CONSTRAINT fk_service_reviews_customer_shop
        FOREIGN KEY (shop_id, customer_id)
        REFERENCES customers(shop_id, id)
        ON DELETE CASCADE
);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_trigger
    WHERE tgname = 'trg_service_reviews_updated_at'
      AND tgrelid = 'service_reviews'::regclass
  ) THEN
    CREATE TRIGGER trg_service_reviews_updated_at
        BEFORE UPDATE ON service_reviews
        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_service_reviews_shop_service_created_at
    ON service_reviews(shop_id, service_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_service_reviews_shop_customer_created_at
    ON service_reviews(shop_id, customer_id, created_at DESC, id DESC);
