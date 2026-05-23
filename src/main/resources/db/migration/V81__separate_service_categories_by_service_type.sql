ALTER TABLE service_categories
    ADD COLUMN IF NOT EXISTS service_type varchar (50) NOT NULL DEFAULT 'GENERAL';

DO
$$
BEGIN
    IF
EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_service_categories_shop_name'
          AND conrelid = 'service_categories'::regclass
    ) THEN
ALTER TABLE service_categories
DROP
CONSTRAINT uq_service_categories_shop_name;
END IF;
END $$;

DO
$$
BEGIN
    IF
NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_service_categories_shop_name_type'
          AND conrelid = 'service_categories'::regclass
    ) THEN
ALTER TABLE service_categories
    ADD CONSTRAINT uq_service_categories_shop_name_type
        UNIQUE (shop_id, name, service_type);
END IF;
END $$;

DO
$$
BEGIN
    IF
NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_service_categories_service_type_valid'
          AND conrelid = 'service_categories'::regclass
    ) THEN
ALTER TABLE service_categories
    ADD CONSTRAINT chk_service_categories_service_type_valid
        CHECK (service_type IN ('GENERAL', 'VETERINARY'));
END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_service_categories_shop_type_active
    ON service_categories(shop_id, service_type, active);

INSERT INTO service_categories (shop_id, name, description, active, sort_order, service_type)
SELECT shops.id,
       categories.name,
       categories.description,
       true,
       categories.sort_order,
       'VETERINARY'
FROM shops
         CROSS JOIN (VALUES ('Tiêm phòng', 'Các dịch vụ tiêm phòng và vaccine cho thú cưng', 10),
                            ('Khám bệnh', 'Các dịch vụ khám lâm sàng và kiểm tra tổng quát', 20),
                            ('Xét nghiệm', 'Các dịch vụ xét nghiệm và chẩn đoán cận lâm sàng', 30),
                            ('Điều trị', 'Các dịch vụ điều trị và can thiệp thú y', 40),
                            ('Tư vấn', 'Các dịch vụ tư vấn sức khỏe và hướng dẫn chăm sóc',
                             50)) AS categories(name, description, sort_order)
    ON CONFLICT (shop_id, name, service_type) DO
UPDATE
SET
    description = EXCLUDED.description, active = EXCLUDED.active, sort_order = EXCLUDED.sort_order;

WITH target_categories AS (SELECT sc.shop_id,
                                  sc.id,
                                  sc.name
                           FROM service_categories sc
                           WHERE sc.service_type = 'VETERINARY')
UPDATE services s
SET category_id = tc.id FROM target_categories tc
WHERE s.shop_id = tc.shop_id
  AND s.service_type = 'VETERINARY'
  AND (
    (s.veterinary_service_type = 'VACCINATION'
  AND tc.name = 'Tiêm phòng')
   OR (s.veterinary_service_type = 'EXAMINATION'
  AND tc.name = 'Khám bệnh')
   OR (s.veterinary_service_type = 'TEST'
  AND tc.name = 'Xét nghiệm')
   OR (s.veterinary_service_type = 'CONSULTATION'
  AND tc.name = 'Tư vấn')
   OR (s.veterinary_service_type IN ('TREATMENT'
    , 'SURGERY'
    , 'OTHER')
  AND tc.name = 'Điều trị')
    );
