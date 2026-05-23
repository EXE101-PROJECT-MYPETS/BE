WITH selected_services AS (SELECT *
                           FROM (SELECT 'SPA_BATH'::varchar(32) AS service_key, s.id AS service_id,
                                        s.base_price,
                                        NULL::bigint AS species_id
                                 FROM services s
                                 WHERE s.shop_id = 1
                                   AND s.service_type = 'GENERAL'
                                   AND s.active = true
                                 ORDER BY s.id LIMIT 1) spa_bath

                           UNION ALL

                           SELECT *
                           FROM (SELECT 'SPA_GROOM'::varchar(32) AS service_key, s.id AS service_id,
                                        s.base_price,
                                        NULL::bigint AS species_id
                                 FROM services s
                                 WHERE s.shop_id = 1
                                   AND s.service_type = 'GENERAL'
                                   AND s.active = true
                                 ORDER BY s.id
                                 OFFSET 1 LIMIT 1) spa_groom

                           UNION ALL

                           SELECT *
                           FROM (SELECT 'VET_EXAM'::varchar(32) AS service_key, s.id AS service_id,
                                        s.base_price,
                                        NULL::bigint AS species_id
                                 FROM services s
                                 WHERE s.shop_id = 1
                                   AND s.service_type = 'VETERINARY'
                                   AND s.veterinary_service_type = 'EXAMINATION'
                                   AND s.active = true
                                 ORDER BY s.id LIMIT 1) vet_exam

                           UNION ALL

                           SELECT *
                           FROM (SELECT 'VET_VACCINE'::varchar(32) AS service_key, s.id AS service_id,
                                        s.base_price,
                                        v.species_id
                                 FROM services s
                                          JOIN vaccines v
                                               ON v.id = s.vaccine_id
                                 WHERE s.shop_id = 1
                                   AND s.service_type = 'VETERINARY'
                                   AND s.veterinary_service_type = 'VACCINATION'
                                   AND s.active = true
                                 ORDER BY s.id LIMIT 1) vet_vaccine

                           UNION ALL

                           SELECT *
                           FROM (SELECT 'VET_TREATMENT'::varchar(32) AS service_key, s.id AS service_id,
                                        s.base_price,
                                        NULL::bigint AS species_id
                                 FROM services s
                                 WHERE s.shop_id = 1
                                   AND s.service_type = 'VETERINARY'
                                   AND s.veterinary_service_type IN ('TREATMENT', 'SURGERY', 'OTHER')
                                   AND s.active = true
                                 ORDER BY s.id LIMIT 1) vet_treatment

                           UNION ALL

                           SELECT *
                           FROM (SELECT 'VET_CONSULT'::varchar(32) AS service_key, s.id AS service_id,
                                        s.base_price,
                                        NULL::bigint AS species_id
                                 FROM services s
                                 WHERE s.shop_id = 1
                                   AND s.service_type = 'VETERINARY'
                                   AND s.veterinary_service_type = 'CONSULTATION'
                                   AND s.active = true
                                 ORDER BY s.id LIMIT 1) vet_consult),
     seed_schedule AS (SELECT *
                       FROM (VALUES ('2026-05-22'::date, 1, '096778899', '08:30'::time, '09:00'::time, 'SPA_BATH'),
                                    ('2026-05-22'::date, 2, '0912345678', '09:15'::time, '10:00'::time, 'VET_EXAM'),
                                    ('2026-05-22'::date, 3, '0987654321', '10:15'::time, '10:45'::time, 'VET_VACCINE'),
                                    ('2026-05-22'::date, 4, '0944556677', '13:30'::time, '14:15'::time, 'SPA_GROOM'),
                                    ('2026-05-22'::date, 5, '0901122334', '15:00'::time, '15:45'::time,
                                     'VET_TREATMENT'),
                                    ('2026-05-22'::date, 6, '0933445566', '16:00'::time, '16:30'::time, 'VET_CONSULT'),
                                    ('2026-05-23'::date, 1, '0955667788', '08:30'::time, '09:00'::time, 'SPA_BATH'),
                                    ('2026-05-23'::date, 2, '096778899', '09:15'::time, '10:00'::time, 'VET_EXAM'),
                                    ('2026-05-23'::date, 3, '0912345678', '10:15'::time, '10:45'::time, 'VET_VACCINE'),
                                    ('2026-05-23'::date, 4, '0987654321', '13:30'::time, '14:15'::time, 'SPA_GROOM'),
                                    ('2026-05-23'::date, 5, '0944556677', '15:00'::time, '15:45'::time,
                                     'VET_TREATMENT'),
                                    ('2026-05-23'::date, 6, '0901122334', '16:00'::time, '16:30'::time,
                                     'VET_CONSULT')) AS v(booking_date, slot_no, customer_phone, start_time, end_time,
                                                          service_key)),
     resolved_bookings AS (SELECT 1::bigint AS shop_id, c.id AS customer_id,
                                  c.user_id,
                                  (ss.booking_date + ss.start_time) AT TIME ZONE 'Asia/Ho_Chi_Minh' AS start_at,
                                  (ss.booking_date + ss.end_time) AT TIME ZONE 'Asia/Ho_Chi_Minh'   AS end_at,
                                  CONCAT(
                                          'V82_BOOKING_',
                                          TO_CHAR(ss.booking_date, 'YYYYMMDD'),
                                          '_',
                                          LPAD(ss.slot_no::text, 2, '0')
                                  )                                                                 AS note
                           FROM seed_schedule ss
                                    JOIN customers c
                                         ON c.shop_id = 1
                                             AND c.phone = ss.customer_phone
                                    JOIN selected_services sv
                                         ON sv.service_key = ss.service_key)
INSERT
INTO bookings (shop_id, user_id, customer_id, start_at, end_at, status, source, note, created_by)
SELECT rb.shop_id,
       rb.user_id,
       rb.customer_id,
       rb.start_at,
       rb.end_at,
       'CONFIRMED'::booking_status, 'STAFF'::booking_source, rb.note,
       NULL
FROM resolved_bookings rb
WHERE EXISTS (SELECT 1
              FROM shops
              WHERE shops.id = rb.shop_id)
  AND NOT EXISTS (SELECT 1
                  FROM bookings b
                  WHERE b.shop_id = rb.shop_id
                    AND b.note = rb.note);

WITH selected_services AS (SELECT *
                           FROM (SELECT 'SPA_BATH'::varchar(32) AS service_key, s.id AS service_id,
                                        s.base_price,
                                        NULL::bigint AS species_id
                                 FROM services s
                                 WHERE s.shop_id = 1
                                   AND s.service_type = 'GENERAL'
                                   AND s.active = true
                                 ORDER BY s.id LIMIT 1) spa_bath

                           UNION ALL

                           SELECT *
                           FROM (SELECT 'SPA_GROOM'::varchar(32) AS service_key, s.id AS service_id,
                                        s.base_price,
                                        NULL::bigint AS species_id
                                 FROM services s
                                 WHERE s.shop_id = 1
                                   AND s.service_type = 'GENERAL'
                                   AND s.active = true
                                 ORDER BY s.id
                                 OFFSET 1 LIMIT 1) spa_groom

                           UNION ALL

                           SELECT *
                           FROM (SELECT 'VET_EXAM'::varchar(32) AS service_key, s.id AS service_id,
                                        s.base_price,
                                        NULL::bigint AS species_id
                                 FROM services s
                                 WHERE s.shop_id = 1
                                   AND s.service_type = 'VETERINARY'
                                   AND s.veterinary_service_type = 'EXAMINATION'
                                   AND s.active = true
                                 ORDER BY s.id LIMIT 1) vet_exam

                           UNION ALL

                           SELECT *
                           FROM (SELECT 'VET_VACCINE'::varchar(32) AS service_key, s.id AS service_id,
                                        s.base_price,
                                        v.species_id
                                 FROM services s
                                          JOIN vaccines v
                                               ON v.id = s.vaccine_id
                                 WHERE s.shop_id = 1
                                   AND s.service_type = 'VETERINARY'
                                   AND s.veterinary_service_type = 'VACCINATION'
                                   AND s.active = true
                                 ORDER BY s.id LIMIT 1) vet_vaccine

                           UNION ALL

                           SELECT *
                           FROM (SELECT 'VET_TREATMENT'::varchar(32) AS service_key, s.id AS service_id,
                                        s.base_price,
                                        NULL::bigint AS species_id
                                 FROM services s
                                 WHERE s.shop_id = 1
                                   AND s.service_type = 'VETERINARY'
                                   AND s.veterinary_service_type IN ('TREATMENT', 'SURGERY', 'OTHER')
                                   AND s.active = true
                                 ORDER BY s.id LIMIT 1) vet_treatment

                           UNION ALL

                           SELECT *
                           FROM (SELECT 'VET_CONSULT'::varchar(32) AS service_key, s.id AS service_id,
                                        s.base_price,
                                        NULL::bigint AS species_id
                                 FROM services s
                                 WHERE s.shop_id = 1
                                   AND s.service_type = 'VETERINARY'
                                   AND s.veterinary_service_type = 'CONSULTATION'
                                   AND s.active = true
                                 ORDER BY s.id LIMIT 1) vet_consult),
     seed_schedule AS (SELECT *
                       FROM (VALUES ('2026-05-22'::date, 1, '096778899', 'SPA_BATH'),
                                    ('2026-05-22'::date, 2, '0912345678', 'VET_EXAM'),
                                    ('2026-05-22'::date, 3, '0987654321', 'VET_VACCINE'),
                                    ('2026-05-22'::date, 4, '0944556677', 'SPA_GROOM'),
                                    ('2026-05-22'::date, 5, '0901122334', 'VET_TREATMENT'),
                                    ('2026-05-22'::date, 6, '0933445566', 'VET_CONSULT'),
                                    ('2026-05-23'::date, 1, '0955667788', 'SPA_BATH'),
                                    ('2026-05-23'::date, 2, '096778899', 'VET_EXAM'),
                                    ('2026-05-23'::date, 3, '0912345678', 'VET_VACCINE'),
                                    ('2026-05-23'::date, 4, '0987654321', 'SPA_GROOM'),
                                    ('2026-05-23'::date, 5, '0944556677', 'VET_TREATMENT'),
                                    ('2026-05-23'::date, 6, '0901122334',
                                     'VET_CONSULT')) AS v(booking_date, slot_no, customer_phone, service_key)),
     resolved_items AS (SELECT b.shop_id,
                               b.id AS booking_id,
                               pet_match.pet_id,
                               sv.service_id,
                               sv.base_price
                        FROM seed_schedule ss
                                 JOIN bookings b
                                      ON b.shop_id = 1
                                          AND b.note = CONCAT(
                                                  'V82_BOOKING_',
                                                  TO_CHAR(ss.booking_date, 'YYYYMMDD'),
                                                  '_',
                                                  LPAD(ss.slot_no::text, 2, '0')
                                                       )
                                 JOIN customers c
                                      ON c.shop_id = 1
                                          AND c.phone = ss.customer_phone
                                 JOIN selected_services sv
                                      ON sv.service_key = ss.service_key
                                 LEFT JOIN LATERAL (
                            SELECT p.id AS pet_id
                            FROM pets p
                            WHERE p.user_id = c.user_id
                              AND (sv.species_id IS NULL OR p.species_id = sv.species_id)
                            ORDER BY p.id
                                LIMIT 1
    ) pet_match
ON true
    )
INSERT
INTO booking_items (shop_id, booking_id, pet_id, item_type, ref_id, qty, unit_price, amount)
SELECT ri.shop_id,
       ri.booking_id,
       ri.pet_id,
       'SERVICE'::booking_item_type, ri.service_id,
       1,
       ri.base_price,
       ri.base_price
FROM resolved_items ri
WHERE NOT EXISTS (SELECT 1
                  FROM booking_items bi
                  WHERE bi.booking_id = ri.booking_id
                    AND bi.item_type = 'SERVICE'::booking_item_type
    AND bi.ref_id = ri.service_id);
