ALTER TABLE prod.pets
    ADD COLUMN IF NOT EXISTS weight_kg numeric (6, 2);

ALTER TABLE prod.pets
DROP
CONSTRAINT IF EXISTS chk_pets_weight_kg_positive;

ALTER TABLE prod.pets
    ADD CONSTRAINT chk_pets_weight_kg_positive
        CHECK (weight_kg IS NULL OR weight_kg > 0);
