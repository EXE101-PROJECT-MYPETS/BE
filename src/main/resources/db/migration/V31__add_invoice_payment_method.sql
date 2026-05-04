ALTER TABLE invoices
    ADD COLUMN IF NOT EXISTS payment_method varchar(30);

ALTER TABLE invoices
    DROP CONSTRAINT IF EXISTS chk_invoices_payment_method;

ALTER TABLE invoices
    ADD CONSTRAINT chk_invoices_payment_method
        CHECK (payment_method IS NULL OR payment_method IN ('CASH', 'BANK_TRANSFER'));
