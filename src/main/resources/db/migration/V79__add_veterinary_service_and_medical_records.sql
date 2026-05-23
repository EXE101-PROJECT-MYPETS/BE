ALTER TABLE services
    ADD COLUMN IF NOT EXISTS service_type varchar (50) NOT NULL DEFAULT 'GENERAL',
    ADD COLUMN IF NOT EXISTS veterinary_service_type varchar (50),
    ADD COLUMN IF NOT EXISTS vaccine_id bigint;

DO
$$
BEGIN
    IF
NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_services_vaccine'
          AND conrelid = 'services'::regclass
    ) THEN
ALTER TABLE services
    ADD CONSTRAINT fk_services_vaccine
        FOREIGN KEY (vaccine_id)
            REFERENCES vaccines (id)
            ON DELETE SET NULL;
END IF;
END $$;

DO
$$
BEGIN
    IF
NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_services_service_type_valid'
          AND conrelid = 'services'::regclass
    ) THEN
ALTER TABLE services
    ADD CONSTRAINT chk_services_service_type_valid
        CHECK (service_type IN ('GENERAL', 'VETERINARY'));
END IF;
END $$;

DO
$$
BEGIN
    IF
NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_services_veterinary_service_type_valid'
          AND conrelid = 'services'::regclass
    ) THEN
ALTER TABLE services
    ADD CONSTRAINT chk_services_veterinary_service_type_valid
        CHECK (
            veterinary_service_type IS NULL
                OR veterinary_service_type IN (
                                               'VACCINATION',
                                               'EXAMINATION',
                                               'TREATMENT',
                                               'TEST',
                                               'SURGERY',
                                               'CONSULTATION',
                                               'OTHER'
                )
            );
END IF;
END $$;

DO
$$
BEGIN
    IF
NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_services_veterinary_fields_consistent'
          AND conrelid = 'services'::regclass
    ) THEN
ALTER TABLE services
    ADD CONSTRAINT chk_services_veterinary_fields_consistent
        CHECK (
            (service_type = 'GENERAL' AND veterinary_service_type IS NULL AND vaccine_id IS NULL)
                OR (service_type = 'VETERINARY' AND (vaccine_id IS NULL OR veterinary_service_type = 'VACCINATION'))
            );
END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_services_shop_service_type_active
    ON services(shop_id, service_type, active);

CREATE INDEX IF NOT EXISTS idx_services_vaccine_id
    ON services(vaccine_id)
    WHERE vaccine_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS pet_medical_records
(
    id
    bigserial
    PRIMARY
    KEY,
    shop_id
    bigint
    NOT
    NULL
    REFERENCES
    shops
(
    id
) ON DELETE CASCADE,
    pet_id bigint NOT NULL REFERENCES pets
(
    id
)
  ON DELETE CASCADE,
    booking_id bigint,
    booking_item_id bigint,
    service_id bigint,
    vaccine_id bigint,
    veterinarian_user_id bigint REFERENCES users
(
    id
)
  ON DELETE SET NULL,
    record_type varchar
(
    50
) NOT NULL DEFAULT 'GENERAL',
    veterinary_service_type varchar
(
    50
),
    performed_at timestamptz NOT NULL DEFAULT now
(
),
    symptoms text,
    diagnosis text,
    treatment text,
    note text,
    follow_up_at timestamptz,
    created_by bigint REFERENCES users
(
    id
)
  ON DELETE SET NULL,
    created_at timestamptz NOT NULL DEFAULT now
(
),
    updated_at timestamptz NOT NULL DEFAULT now
(
)
    );

DO
$$
BEGIN
    IF
NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_pet_medical_records_booking_shop'
          AND conrelid = 'pet_medical_records'::regclass
    ) THEN
ALTER TABLE pet_medical_records
    ADD CONSTRAINT fk_pet_medical_records_booking_shop
        FOREIGN KEY (shop_id, booking_id)
            REFERENCES bookings (shop_id, id)
            ON DELETE SET NULL;
END IF;
END $$;

DO
$$
BEGIN
    IF
NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_pet_medical_records_booking_item'
          AND conrelid = 'pet_medical_records'::regclass
    ) THEN
ALTER TABLE pet_medical_records
    ADD CONSTRAINT fk_pet_medical_records_booking_item
        FOREIGN KEY (booking_item_id)
            REFERENCES booking_items (id)
            ON DELETE SET NULL;
END IF;
END $$;

DO
$$
BEGIN
    IF
NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_pet_medical_records_service'
          AND conrelid = 'pet_medical_records'::regclass
    ) THEN
ALTER TABLE pet_medical_records
    ADD CONSTRAINT fk_pet_medical_records_service
        FOREIGN KEY (service_id)
            REFERENCES services (id)
            ON DELETE SET NULL;
END IF;
END $$;

DO
$$
BEGIN
    IF
NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_pet_medical_records_vaccine'
          AND conrelid = 'pet_medical_records'::regclass
    ) THEN
ALTER TABLE pet_medical_records
    ADD CONSTRAINT fk_pet_medical_records_vaccine
        FOREIGN KEY (vaccine_id)
            REFERENCES vaccines (id)
            ON DELETE SET NULL;
END IF;
END $$;

DO
$$
BEGIN
    IF
NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_pet_medical_records_record_type_valid'
          AND conrelid = 'pet_medical_records'::regclass
    ) THEN
ALTER TABLE pet_medical_records
    ADD CONSTRAINT chk_pet_medical_records_record_type_valid
        CHECK (record_type IN ('GENERAL', 'VETERINARY'));
END IF;
END $$;

DO
$$
BEGIN
    IF
NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_pet_medical_records_veterinary_service_type_valid'
          AND conrelid = 'pet_medical_records'::regclass
    ) THEN
ALTER TABLE pet_medical_records
    ADD CONSTRAINT chk_pet_medical_records_veterinary_service_type_valid
        CHECK (
            veterinary_service_type IS NULL
                OR veterinary_service_type IN (
                                               'VACCINATION',
                                               'EXAMINATION',
                                               'TREATMENT',
                                               'TEST',
                                               'SURGERY',
                                               'CONSULTATION',
                                               'OTHER'
                )
            );
END IF;
END $$;

DO
$$
BEGIN
    IF
NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_pet_medical_records_veterinary_fields_consistent'
          AND conrelid = 'pet_medical_records'::regclass
    ) THEN
ALTER TABLE pet_medical_records
    ADD CONSTRAINT chk_pet_medical_records_veterinary_fields_consistent
        CHECK (
            (record_type = 'GENERAL' AND veterinary_service_type IS NULL AND vaccine_id IS NULL)
                OR (record_type = 'VETERINARY' AND (vaccine_id IS NULL OR veterinary_service_type = 'VACCINATION'))
            );
END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_pet_medical_records_shop_pet_performed
    ON pet_medical_records(shop_id, pet_id, performed_at DESC);

CREATE INDEX IF NOT EXISTS idx_pet_medical_records_booking_item
    ON pet_medical_records(booking_item_id)
    WHERE booking_item_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_pet_medical_records_service
    ON pet_medical_records(service_id)
    WHERE service_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_pet_medical_records_booking_item
    ON pet_medical_records(booking_item_id)
    WHERE booking_item_id IS NOT NULL;

DO
$$
BEGIN
    IF
NOT EXISTS (
        SELECT 1
        FROM pg_trigger
        WHERE tgname = 'trg_pet_medical_records_updated_at'
          AND tgrelid = 'pet_medical_records'::regclass
    ) THEN
CREATE TRIGGER trg_pet_medical_records_updated_at
    BEFORE UPDATE
    ON pet_medical_records
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
END IF;
END $$;

ALTER TABLE pet_vaccinations
    ADD COLUMN IF NOT EXISTS shop_id bigint,
    ADD COLUMN IF NOT EXISTS booking_id bigint,
    ADD COLUMN IF NOT EXISTS booking_item_id bigint,
    ADD COLUMN IF NOT EXISTS service_id bigint,
    ADD COLUMN IF NOT EXISTS medical_record_id bigint;

DO
$$
BEGIN
    IF
NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_pet_vaccinations_shop'
          AND conrelid = 'pet_vaccinations'::regclass
    ) THEN
ALTER TABLE pet_vaccinations
    ADD CONSTRAINT fk_pet_vaccinations_shop
        FOREIGN KEY (shop_id)
            REFERENCES shops (id)
            ON DELETE SET NULL;
END IF;
END $$;

DO
$$
BEGIN
    IF
NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_pet_vaccinations_booking_shop'
          AND conrelid = 'pet_vaccinations'::regclass
    ) THEN
ALTER TABLE pet_vaccinations
    ADD CONSTRAINT fk_pet_vaccinations_booking_shop
        FOREIGN KEY (shop_id, booking_id)
            REFERENCES bookings (shop_id, id)
            ON DELETE SET NULL;
END IF;
END $$;

DO
$$
BEGIN
    IF
NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_pet_vaccinations_booking_item'
          AND conrelid = 'pet_vaccinations'::regclass
    ) THEN
ALTER TABLE pet_vaccinations
    ADD CONSTRAINT fk_pet_vaccinations_booking_item
        FOREIGN KEY (booking_item_id)
            REFERENCES booking_items (id)
            ON DELETE SET NULL;
END IF;
END $$;

DO
$$
BEGIN
    IF
NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_pet_vaccinations_service'
          AND conrelid = 'pet_vaccinations'::regclass
    ) THEN
ALTER TABLE pet_vaccinations
    ADD CONSTRAINT fk_pet_vaccinations_service
        FOREIGN KEY (service_id)
            REFERENCES services (id)
            ON DELETE SET NULL;
END IF;
END $$;

DO
$$
BEGIN
    IF
NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_pet_vaccinations_medical_record'
          AND conrelid = 'pet_vaccinations'::regclass
    ) THEN
ALTER TABLE pet_vaccinations
    ADD CONSTRAINT fk_pet_vaccinations_medical_record
        FOREIGN KEY (medical_record_id)
            REFERENCES pet_medical_records (id)
            ON DELETE SET NULL;
END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_pet_vaccinations_booking_item
    ON pet_vaccinations(booking_item_id)
    WHERE booking_item_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_pet_vaccinations_service
    ON pet_vaccinations(service_id)
    WHERE service_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_pet_vaccinations_shop_pet_date
    ON pet_vaccinations(shop_id, pet_id, vaccinated_at)
    WHERE shop_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_pet_vaccinations_booking_item
    ON pet_vaccinations(booking_item_id)
    WHERE booking_item_id IS NOT NULL;
