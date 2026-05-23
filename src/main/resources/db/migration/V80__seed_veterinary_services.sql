WITH selected_shops AS (SELECT id AS shop_id
                        FROM shops
                        WHERE id IN (1, 2)),
     health_categories AS (SELECT sc.shop_id,
                                  sc.id AS category_id
                           FROM service_categories sc
                                    JOIN selected_shops ss ON ss.shop_id = sc.shop_id
                           WHERE sc.name = 'Chăm sóc sức khỏe'),
     seed_services AS (SELECT *
                       FROM (VALUES
                                 -- Vaccination services for dogs (species_id = 2)
                                 ('Tiêm vaccine dại cho chó', 20, 180000, true, 'VETERINARY', 'VACCINATION', 16,
                                  '/uploads/shops/default/services/veterinary-rabies-dog.jpg'),
                                 ('Tiêm vaccine 5 bệnh cho chó', 25, 280000, true, 'VETERINARY', 'VACCINATION', 17,
                                  '/uploads/shops/default/services/veterinary-v5-dog.jpg'),
                                 ('Tiêm vaccine 7 bệnh cho chó', 25, 320000, true, 'VETERINARY', 'VACCINATION', 18,
                                  '/uploads/shops/default/services/veterinary-v7-dog.jpg'),
                                 ('Tiêm vaccine Parvo cho chó', 20, 220000, true, 'VETERINARY', 'VACCINATION', 19,
                                  '/uploads/shops/default/services/veterinary-parvo-dog.jpg'),
                                 ('Tiêm vaccine Care cho chó', 20, 220000, true, 'VETERINARY', 'VACCINATION', 20,
                                  '/uploads/shops/default/services/veterinary-care-dog.jpg'),
                                 ('Tiêm vaccine ho cũi chó', 20, 200000, true, 'VETERINARY', 'VACCINATION', 21,
                                  '/uploads/shops/default/services/veterinary-kennel-cough-dog.jpg'),
                                 ('Tiêm vaccine Leptospira cho chó', 20, 230000, true, 'VETERINARY', 'VACCINATION', 22,
                                  '/uploads/shops/default/services/veterinary-lepto-dog.jpg'),

                                 -- Vaccination services for cats (species_id = 3)
                                 ('Tiêm vaccine dại cho mèo', 20, 180000, true, 'VETERINARY', 'VACCINATION', 23,
                                  '/uploads/shops/default/services/veterinary-rabies-cat.jpg'),
                                 ('Tiêm vaccine 3 bệnh cho mèo', 25, 260000, true, 'VETERINARY', 'VACCINATION', 24,
                                  '/uploads/shops/default/services/veterinary-v3-cat.jpg'),
                                 ('Tiêm vaccine 4 bệnh cho mèo', 25, 300000, true, 'VETERINARY', 'VACCINATION', 25,
                                  '/uploads/shops/default/services/veterinary-v4-cat.jpg'),
                                 ('Tiêm vaccine giảm bạch cầu mèo', 20, 240000, true, 'VETERINARY', 'VACCINATION', 26,
                                  '/uploads/shops/default/services/veterinary-panleukopenia-cat.jpg'),
                                 ('Tiêm vaccine viêm mũi khí quản mèo', 20, 230000, true, 'VETERINARY', 'VACCINATION',
                                  27, '/uploads/shops/default/services/veterinary-rhinotracheitis-cat.jpg'),
                                 ('Tiêm vaccine Calicivirus mèo', 20, 230000, true, 'VETERINARY', 'VACCINATION', 28,
                                  '/uploads/shops/default/services/veterinary-calicivirus-cat.jpg'),

                                 -- General veterinary services
                                 ('Khám tổng quát thú cưng', 30, 150000, true, 'VETERINARY', 'EXAMINATION', NULL,
                                  '/uploads/shops/default/services/veterinary-general-exam.jpg'),
                                 ('Tư vấn thú y', 30, 120000, true, 'VETERINARY', 'CONSULTATION', NULL,
                                  '/uploads/shops/default/services/veterinary-consultation.jpg'),
                                 ('Điều trị da liễu thú cưng', 45, 250000, true, 'VETERINARY', 'TREATMENT', NULL,
                                  '/uploads/shops/default/services/veterinary-dermatology.jpg'),
                                 ('Xét nghiệm ký sinh trùng', 35, 180000, true, 'VETERINARY', 'TEST', NULL,
                                  '/uploads/shops/default/services/veterinary-parasite-test.jpg')) AS v(name,
                                                                                                        duration_min,
                                                                                                        base_price,
                                                                                                        active,
                                                                                                        service_type,
                                                                                                        veterinary_service_type,
                                                                                                        vaccine_id,
                                                                                                        image_url))
INSERT
INTO services (
    shop_id,
    name,
    duration_min,
    base_price,
    active,
    category_id,
    service_type,
    veterinary_service_type,
    vaccine_id,
    image_url
)
SELECT hc.shop_id,
       ss.name,
       ss.duration_min,
       ss.base_price,
       ss.active,
       hc.category_id,
       ss.service_type::varchar(50), ss.veterinary_service_type::varchar(50), ss.vaccine_id,
       ss.image_url
FROM health_categories hc
         CROSS JOIN seed_services ss
    ON CONFLICT (shop_id, name) DO
UPDATE
SET
    duration_min = EXCLUDED.duration_min, base_price = EXCLUDED.base_price, active = EXCLUDED.active, category_id = EXCLUDED.category_id, service_type = EXCLUDED.service_type, veterinary_service_type = EXCLUDED.veterinary_service_type, vaccine_id = EXCLUDED.vaccine_id, image_url = EXCLUDED.image_url;

SELECT setval(
               pg_get_serial_sequence('services', 'id'),
               COALESCE((SELECT MAX(id) FROM services), 1),
               true
       );
