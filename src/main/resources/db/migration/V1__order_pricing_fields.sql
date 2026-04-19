ALTER TABLE orders
    ADD COLUMN subtotal              NUMERIC(10, 2) NOT NULL DEFAULT 0,
    ADD COLUMN cgst                  NUMERIC(10, 2) NOT NULL DEFAULT 0,
    ADD COLUMN sgst                  NUMERIC(10, 2) NOT NULL DEFAULT 0,
    ADD COLUMN igst                  NUMERIC(10, 2) NOT NULL DEFAULT 0,
    ADD COLUMN tax_amount            NUMERIC(10, 2) NOT NULL DEFAULT 0,
    ADD COLUMN platform_fee          NUMERIC(10, 2) NOT NULL DEFAULT 0,
    ADD COLUMN payment_terminal_fee  NUMERIC(10, 2) NOT NULL DEFAULT 0,
    ADD COLUMN total_service_fee     NUMERIC(10, 2) NOT NULL DEFAULT 0;

UPDATE orders SET subtotal = total_amount WHERE subtotal = 0;
