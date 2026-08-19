ALTER TABLE vendors
    ADD COLUMN subscription_monthly_price NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN subscription_paid_through  DATE,
    ADD COLUMN subscription_status        VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE';

CREATE TABLE subscription_payments (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    vendor_id         UUID        NOT NULL REFERENCES vendors(id),
    amount            NUMERIC(10,2) NOT NULL,
    payment_reference VARCHAR(255),
    paid_for_month    DATE        NOT NULL,
    paid_on           DATE        NOT NULL,
    admin_note        TEXT,
    created_at        TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE INDEX idx_subscription_payments_vendor_id ON subscription_payments(vendor_id);
