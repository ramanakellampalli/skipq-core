-- Freeze discount info on order items at placement time for immutable audit trail
ALTER TABLE order_items
    ADD COLUMN original_price  NUMERIC(10,2) NOT NULL DEFAULT 0,
    ADD COLUMN discount_amount NUMERIC(10,2) NOT NULL DEFAULT 0,
    ADD COLUMN discount_id     UUID REFERENCES discounts(id);
