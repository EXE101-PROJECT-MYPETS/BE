CREATE TABLE IF NOT EXISTS product_categories (
    id bigserial PRIMARY KEY,
    shop_id bigint NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    name varchar(100) NOT NULL,
    description text,
    active boolean NOT NULL DEFAULT true,
    sort_order int NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT uq_product_categories_shop_name UNIQUE (shop_id, name),
    CONSTRAINT uq_product_categories_shop_id UNIQUE (shop_id, id)
);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_trigger
    WHERE tgname = 'trg_product_categories_updated_at'
      AND tgrelid = 'product_categories'::regclass
  ) THEN
    CREATE TRIGGER trg_product_categories_updated_at
        BEFORE UPDATE ON product_categories
        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
END $$;

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS category_id bigint;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'fk_products_category_shop'
      AND conrelid = 'products'::regclass
  ) THEN
    ALTER TABLE products
        ADD CONSTRAINT fk_products_category_shop
        FOREIGN KEY (shop_id, category_id)
        REFERENCES product_categories(shop_id, id)
        ON DELETE RESTRICT;
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_product_categories_shop_active
    ON product_categories(shop_id, active);

CREATE INDEX IF NOT EXISTS idx_product_categories_shop_sort_order
    ON product_categories(shop_id, sort_order);

CREATE INDEX IF NOT EXISTS idx_products_shop_category_active
    ON products(shop_id, category_id, active);

CREATE TABLE IF NOT EXISTS product_images (
    id bigserial PRIMARY KEY,
    shop_id bigint NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    product_id bigint NOT NULL,
    image_url text NOT NULL,
    sort_order int NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT fk_product_images_product_shop
    FOREIGN KEY (shop_id, product_id)
    REFERENCES products(shop_id, id)
    ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_product_images_shop_product_sort
    ON product_images(shop_id, product_id, sort_order);
