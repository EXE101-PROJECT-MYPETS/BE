DROP INDEX IF EXISTS prod.idx_pet_vaccinations_shop_pet_date;

ALTER TABLE prod.pet_vaccinations
DROP
CONSTRAINT IF EXISTS fk_pet_vaccinations_pet_shop;

ALTER TABLE prod.pet_vaccinations
DROP
CONSTRAINT IF EXISTS pet_vaccinations_shop_id_fkey;

ALTER TABLE prod.pet_vaccinations
DROP
COLUMN IF EXISTS shop_id;

CREATE INDEX IF NOT EXISTS idx_pet_vaccinations_pet_date
    ON prod.pet_vaccinations(pet_id, vaccinated_at);
