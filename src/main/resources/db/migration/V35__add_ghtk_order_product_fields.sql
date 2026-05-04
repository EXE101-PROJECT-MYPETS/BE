ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS shipping_province varchar(100),
    ADD COLUMN IF NOT EXISTS shipping_district varchar(100),
    ADD COLUMN IF NOT EXISTS shipping_ward varchar(100),
    ADD COLUMN IF NOT EXISTS shipping_street varchar(255),
    ADD COLUMN IF NOT EXISTS shipping_hamlet varchar(255);

ALTER TABLE orders
    ADD CONSTRAINT chk_orders_shipping_province_not_blank
        CHECK (shipping_province IS NULL OR length(btrim(shipping_province)) > 0),
    ADD CONSTRAINT chk_orders_shipping_district_not_blank
        CHECK (shipping_district IS NULL OR length(btrim(shipping_district)) > 0),
    ADD CONSTRAINT chk_orders_shipping_ward_not_blank
        CHECK (shipping_ward IS NULL OR length(btrim(shipping_ward)) > 0),
    ADD CONSTRAINT chk_orders_shipping_street_not_blank
        CHECK (shipping_street IS NULL OR length(btrim(shipping_street)) > 0),
    ADD CONSTRAINT chk_orders_shipping_hamlet_not_blank
        CHECK (shipping_hamlet IS NULL OR length(btrim(shipping_hamlet)) > 0);

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS weight_kg numeric(10, 3) NOT NULL DEFAULT 0.100;

ALTER TABLE products
    ADD CONSTRAINT chk_products_weight_kg_positive
        CHECK (weight_kg > 0);
