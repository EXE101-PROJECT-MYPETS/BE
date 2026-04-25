ALTER TABLE invoices
  ADD COLUMN IF NOT EXISTS order_id bigint;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'fk_invoices_order_shop'
  ) THEN
    ALTER TABLE invoices
      ADD CONSTRAINT fk_invoices_order_shop
      FOREIGN KEY (shop_id, order_id)
      REFERENCES orders(shop_id, id)
      ON DELETE SET NULL;
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'chk_invoices_single_source'
  ) THEN
    ALTER TABLE invoices
      ADD CONSTRAINT chk_invoices_single_source
      CHECK (booking_id IS NULL OR order_id IS NULL);
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_invoices_shop_order
  ON invoices(shop_id, order_id);
