-- Prevent duplicate payouts for the same vendor + cutoff window.
-- If the settlement job runs twice in the same day, the second INSERT fails
-- with a unique constraint violation instead of silently doubling up.
ALTER TABLE vendor_payouts
    ADD CONSTRAINT uq_payout_vendor_cutoff UNIQUE (vendor_id, settlement_cutoff_at);

-- Partial index speeds up the "find unsettled, un-reserved entries" query
-- that the settlement job runs on every execution.
CREATE INDEX idx_ledger_unreserved
    ON ledger_entries (vendor_id, created_at)
    WHERE settled = false AND payout_id IS NULL;
