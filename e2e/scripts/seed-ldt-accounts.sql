-- LDT test account seed
-- Run this directly in the Neon SQL Editor (dev project).
--
-- Replace <STUDENT_HASH> and <GENERAL_HASH> with BCrypt hashes before running.
-- Generate a hash: python3 -c "import bcrypt; print(bcrypt.hashpw(b'<password>', bcrypt.gensalt(10)).decode())"

-- STUDENT account — campus resolved from srmap.edu.in domain
INSERT INTO users (id, name, email, password_hash, role, campus_id, email_verified, created_at)
SELECT
    gen_random_uuid(),
    'LDT Student',
    'student-ldt@srmap.edu.in',
    '<STUDENT_HASH>',
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
    '<GENERAL_HASH>',
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
