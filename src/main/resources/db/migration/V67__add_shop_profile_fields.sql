ALTER TABLE prod.shops
    ADD COLUMN IF NOT EXISTS phone varchar (50),
    ADD COLUMN IF NOT EXISTS email varchar (255),
    ADD COLUMN IF NOT EXISTS description text,
    ADD COLUMN IF NOT EXISTS opening_hours varchar (20),
    ADD COLUMN IF NOT EXISTS closing_hours varchar (20),
    ADD COLUMN IF NOT EXISTS facebook_url text;
