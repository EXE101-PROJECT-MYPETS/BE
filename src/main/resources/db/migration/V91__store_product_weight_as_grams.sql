ALTER TABLE prod.products
    ALTER COLUMN weight_kg SET DEFAULT 100;

UPDATE prod.products
SET weight_kg = 100
WHERE weight_kg IS DISTINCT
FROM 100;
