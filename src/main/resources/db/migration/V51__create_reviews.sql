CREATE TABLE IF NOT EXISTS reviews (
    id bigserial PRIMARY KEY,
    shop_id bigint NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    product_id bigint NOT NULL,
    customer_id bigint NOT NULL,
    rating int NOT NULL,
    comment text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT chk_reviews_rating_range CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT uq_reviews_shop_product_customer UNIQUE (shop_id, product_id, customer_id),
    CONSTRAINT fk_reviews_product_shop
        FOREIGN KEY (shop_id, product_id)
        REFERENCES products(shop_id, id)
        ON DELETE CASCADE,
    CONSTRAINT fk_reviews_customer_shop
        FOREIGN KEY (shop_id, customer_id)
        REFERENCES customers(shop_id, id)
        ON DELETE CASCADE
);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_trigger
    WHERE tgname = 'trg_reviews_updated_at'
      AND tgrelid = 'reviews'::regclass
  ) THEN
    CREATE TRIGGER trg_reviews_updated_at
        BEFORE UPDATE ON reviews
        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_reviews_shop_product_created_at
    ON reviews(shop_id, product_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_reviews_shop_customer_created_at
    ON reviews(shop_id, customer_id, created_at DESC, id DESC);