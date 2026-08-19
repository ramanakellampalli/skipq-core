-- subscription_status is no longer needed — PAST_DUE is computed on read,
-- and vendor suspension uses accountStatus (which already exists).
ALTER TABLE vendors DROP CONSTRAINT IF EXISTS chk_subscription_status;
ALTER TABLE vendors DROP COLUMN IF EXISTS subscription_status;
