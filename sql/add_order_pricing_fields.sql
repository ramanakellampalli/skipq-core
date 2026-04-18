-- Run this against the orders table before deploying feature/order-pricing
ALTER TABLE orders
    ADD COLUMN subtotal              NUMERIC(10, 2) NOT NULL DEFAULT 0,
    ADD COLUMN cgst                  NUMERIC(10, 2) NOT NULL DEFAULT 0,
    ADD COLUMN sgst                  NUMERIC(10, 2) NOT NULL DEFAULT 0,
    ADD COLUMN igst                  NUMERIC(10, 2) NOT NULL DEFAULT 0,
    ADD COLUMN tax_amount            NUMERIC(10, 2) NOT NULL DEFAULT 0,
    ADD COLUMN platform_fee          NUMERIC(10, 2) NOT NULL DEFAULT 0,
    ADD COLUMN payment_terminal_fee  NUMERIC(10, 2) NOT NULL DEFAULT 0,
    ADD COLUMN total_service_fee     NUMERIC(10, 2) NOT NULL DEFAULT 0;

-- Backfill existing orders: treat total_amount as subtotal, zero out fees
-- (existing test orders won't have accurate breakdowns, that's fine)
UPDATE orders SET subtotal = total_amount WHERE subtotal = 0;
