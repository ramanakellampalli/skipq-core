ALTER TABLE vendors
    ADD CONSTRAINT chk_subscription_status
    CHECK (subscription_status IN ('ACTIVE', 'SUSPENDED'));
