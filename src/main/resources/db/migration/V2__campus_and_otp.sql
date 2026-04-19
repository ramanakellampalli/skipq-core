CREATE TABLE campuses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    email_domain VARCHAR(100) NOT NULL UNIQUE
);

INSERT INTO campuses (name, email_domain)
VALUES ('SRM AP', 'srmap.edu.in');

ALTER TABLE users
    ADD COLUMN campus_id UUID REFERENCES campuses(id);

ALTER TABLE vendors
    ADD COLUMN campus_id UUID REFERENCES campuses(id);

UPDATE vendors
SET campus_id = (SELECT id FROM campuses WHERE email_domain = 'srmap.edu.in');

ALTER TABLE vendors
    ALTER COLUMN campus_id SET NOT NULL;

ALTER TABLE users
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN otp_code VARCHAR(6),
    ADD COLUMN otp_expires_at TIMESTAMP;
