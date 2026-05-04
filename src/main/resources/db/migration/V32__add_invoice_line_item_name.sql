ALTER TABLE invoice_lines
    ADD COLUMN IF NOT EXISTS item_name varchar(255);
