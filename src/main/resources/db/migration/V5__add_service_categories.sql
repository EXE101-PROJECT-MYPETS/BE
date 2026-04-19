CREATE TABLE IF NOT EXISTS service_categories (
    id bigserial PRIMARY KEY,
    shop_id bigint NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    name varchar(100) NOT NULL,
    description text,
    active boolean NOT NULL DEFAULT true,
    sort_order int NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT uq_service_categories_shop_name UNIQUE (shop_id, name),
    CONSTRAINT uq_service_categories_shop_id UNIQUE (shop_id, id)
);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_trigger
    WHERE tgname = 'trg_service_categories_updated_at'
      AND tgrelid = 'service_categories'::regclass
  ) THEN
    CREATE TRIGGER trg_service_categories_updated_at
        BEFORE UPDATE ON service_categories
        FOR EACH ROW EXECUTE FUNCTION set_updated_at();
  END IF;
END $$;

ALTER TABLE services
    ADD COLUMN IF NOT EXISTS category_id bigint;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'fk_services_category_shop'
      AND conrelid = 'services'::regclass
  ) THEN
    ALTER TABLE services
        ADD CONSTRAINT fk_services_category_shop
        FOREIGN KEY (shop_id, category_id)
        REFERENCES service_categories(shop_id, id)
        ON DELETE RESTRICT;
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_service_categories_shop_active
    ON service_categories(shop_id, active);

CREATE INDEX IF NOT EXISTS idx_service_categories_shop_sort_order
    ON service_categories(shop_id, sort_order);

CREATE INDEX IF NOT EXISTS idx_services_shop_category_active
    ON services(shop_id, category_id, active);
