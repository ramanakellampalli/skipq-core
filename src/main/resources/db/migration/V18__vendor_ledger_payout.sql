-- Vendor ledger and payout system
-- Replaces Razorpay Route per-order transfers with internal ledger + daily batch settlement.

-- vendor_payouts must exist before ledger_entries (FK reference)
CREATE TABLE vendor_payouts (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vendor_id               UUID NOT NULL REFERENCES vendors(id),
    amount                  NUMERIC(10, 2) NOT NULL,
    settlement_start_at     TIMESTAMP NOT NULL,
    settlement_cutoff_at    TIMESTAMP NOT NULL,
    status                  VARCHAR(10)  NOT NULL DEFAULT 'PENDING',
    payout_reference        VARCHAR(255),
    admin_note              TEXT,
    created_at              TIMESTAMP NOT NULL DEFAULT now()
);

-- Source of truth for all vendor money movement
CREATE TABLE ledger_entries (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vendor_id   UUID NOT NULL REFERENCES vendors(id),
    order_id    UUID          REFERENCES orders(id),
    payout_id   UUID          REFERENCES vendor_payouts(id),
    amount      NUMERIC(10, 2) NOT NULL,
    type        VARCHAR(20)  NOT NULL,
    settled     BOOLEAN      NOT NULL DEFAULT false,
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),

    CONSTRAINT uq_ledger_order_type UNIQUE (order_id, type)
);

-- Read cache — always updated in same transaction as ledger_entries
CREATE TABLE vendor_ledger (
    vendor_id           UUID PRIMARY KEY REFERENCES vendors(id),
    available_balance   NUMERIC(10, 2) NOT NULL DEFAULT 0,
    updated_at          TIMESTAMP NOT NULL DEFAULT now()
);

-- Seed a vendor_ledger row for every existing vendor
INSERT INTO vendor_ledger (vendor_id, available_balance)
SELECT id, 0 FROM vendors;
