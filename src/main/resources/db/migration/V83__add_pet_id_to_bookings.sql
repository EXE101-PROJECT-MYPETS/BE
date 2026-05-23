ALTER TABLE bookings
    ADD COLUMN pet_id BIGINT;

ALTER TABLE bookings
    ADD CONSTRAINT fk_bookings_pet
        FOREIGN KEY (pet_id) REFERENCES pets (id)
            ON DELETE SET NULL;

CREATE INDEX idx_bookings_pet_id ON bookings (pet_id);

UPDATE bookings b
SET pet_id = src.pet_id FROM (
         SELECT bi.booking_id, MIN(bi.pet_id) AS pet_id
         FROM booking_items bi
                  JOIN services s
                       ON s.id = bi.ref_id
                           AND bi.item_type = 'SERVICE'
         WHERE bi.pet_id IS NOT NULL
           AND s.service_type = 'VETERINARY'
         GROUP BY bi.booking_id
     ) src
WHERE b.id = src.booking_id
  AND b.pet_id IS NULL;
