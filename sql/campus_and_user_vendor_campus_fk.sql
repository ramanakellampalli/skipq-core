-- Step 1: Create campuses table
CREATE TABLE campuses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    email_domain VARCHAR(100) NOT NULL UNIQUE
);

-- Step 2: Seed SRM AP campus
INSERT INTO campuses (name, email_domain)
VALUES ('SRM AP', 'srmap.edu.in');

-- Step 3: Add campus_id to users (nullable — admins/vendors have no campus)
ALTER TABLE users
    ADD COLUMN campus_id UUID REFERENCES campuses(id);

-- Step 4: Add campus_id to vendors
-- Backfill existing vendors to SRM AP before adding NOT NULL constraint
ALTER TABLE vendors
    ADD COLUMN campus_id UUID REFERENCES campuses(id);

UPDATE vendors
SET campus_id = (SELECT id FROM campuses WHERE email_domain = 'srmap.edu.in');

ALTER TABLE vendors
    ALTER COLUMN campus_id SET NOT NULL;

-- Step 5: Add OTP + email verification fields to users
ALTER TABLE users
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN otp_code VARCHAR(6),
    ADD COLUMN otp_expires_at TIMESTAMP;
