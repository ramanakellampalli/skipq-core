-- LDT test account seed
-- Run this directly in the Neon SQL Editor (dev project).
--
-- Default password for both accounts: SkipQLDT2026!
-- Update the hashes below if you change the password:
--   python3 -c "import bcrypt; print(bcrypt.hashpw(b'<pwd>', bcrypt.gensalt(10)).decode())"

-- STUDENT account — campus resolved from srmap.edu.in domain
INSERT INTO users (id, name, email, password_hash, role, campus_id, email_verified, created_at)
SELECT
    gen_random_uuid(),
    'LDT Student',
    'student-ldt@srmap.edu.in',
    '$2b$10$UqEIZPmib1LPB6Db/OGPOeTsDjcxBLhegGtzKhDpbMuseg5zLry5i',
    'STUDENT',
    (SELECT id FROM campuses WHERE email_domain = 'srmap.edu.in' LIMIT 1),
    true,
    NOW()
ON CONFLICT (email) DO UPDATE
    SET password_hash  = EXCLUDED.password_hash,
        role           = 'STUDENT',
        campus_id      = EXCLUDED.campus_id,
        email_verified = true;

-- GENERAL account — no campus
INSERT INTO users (id, name, email, password_hash, role, campus_id, email_verified, created_at)
VALUES (
    gen_random_uuid(),
    'LDT General',
    'general-ldt@gmail.com',
    '$2b$10$UqEIZPmib1LPB6Db/OGPOeTsDjcxBLhegGtzKhDpbMuseg5zLry5i',
    'GENERAL',
    NULL,
    true,
    NOW()
)
ON CONFLICT (email) DO UPDATE
    SET password_hash  = EXCLUDED.password_hash,
        role           = 'GENERAL',
        campus_id      = NULL,
        email_verified = true;

-- Verify
SELECT email, role, campus_id IS NOT NULL AS has_campus, email_verified
FROM users
WHERE email IN ('student-ldt@srmap.edu.in', 'general-ldt@gmail.com')
ORDER BY role;
