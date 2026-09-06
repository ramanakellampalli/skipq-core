#!/usr/bin/env bash
# Seeds LDT test accounts directly in the dev Neon database.
#
# Usage:
#   cp e2e/.env.ldt.example e2e/.env.ldt
#   # fill in e2e/.env.ldt
#   source e2e/.env.ldt && bash e2e/scripts/seed-ldt-accounts.sh
#
# Required env vars (set in .env.ldt or export manually):
#   DATABASE_URL           — psql-compatible URL: postgres://user:pass@host/db
#   LDT_STUDENT_EMAIL      — e.g. student-ldt@srmap.edu.in
#   LDT_STUDENT_PASSWORD   — password for the STUDENT test account
#   LDT_GENERAL_EMAIL      — e.g. general-ldt@gmail.com
#   LDT_GENERAL_PASSWORD   — password for the GENERAL test account
#
# Prerequisites:
#   brew install libpq && brew link libpq --force
#   pip3 install bcrypt

set -euo pipefail

# ── Dependency checks ────────────────────────────────────────────────────────

if ! command -v psql &>/dev/null; then
  echo "ERROR: psql not found."
  echo "  brew install libpq && brew link libpq --force"
  exit 1
fi

if ! python3 -c "import bcrypt" 2>/dev/null; then
  echo "ERROR: Python bcrypt module not found."
  echo "  pip3 install bcrypt"
  exit 1
fi

# ── Required env vars ────────────────────────────────────────────────────────

: "${DATABASE_URL:?DATABASE_URL is required (postgres://user:pass@host/db)}"
: "${LDT_STUDENT_EMAIL:?LDT_STUDENT_EMAIL is required}"
: "${LDT_STUDENT_PASSWORD:?LDT_STUDENT_PASSWORD is required}"
: "${LDT_GENERAL_EMAIL:?LDT_GENERAL_EMAIL is required}"
: "${LDT_GENERAL_PASSWORD:?LDT_GENERAL_PASSWORD is required}"

# ── Hash passwords ───────────────────────────────────────────────────────────

hash_bcrypt() {
  python3 - "$1" <<'PYEOF'
import sys, bcrypt
pw = sys.argv[1].encode()
print(bcrypt.hashpw(pw, bcrypt.gensalt(10)).decode())
PYEOF
}

echo "Hashing passwords (this takes a moment)..."
STUDENT_HASH=$(hash_bcrypt "$LDT_STUDENT_PASSWORD")
GENERAL_HASH=$(hash_bcrypt "$LDT_GENERAL_PASSWORD")

# ── Campus lookup ────────────────────────────────────────────────────────────

STUDENT_DOMAIN="${LDT_STUDENT_EMAIL#*@}"
echo "Resolving campus for domain: $STUDENT_DOMAIN"

CAMPUS_ID=$(psql "$DATABASE_URL" -t -A -c \
  "SELECT id FROM campuses WHERE email_domain = '$STUDENT_DOMAIN' LIMIT 1;")

if [ -z "$CAMPUS_ID" ]; then
  echo ""
  echo "ERROR: No campus found for domain '$STUDENT_DOMAIN'"
  echo ""
  echo "Available campuses:"
  psql "$DATABASE_URL" -c "SELECT id, name, email_domain FROM campuses ORDER BY name;"
  echo ""
  echo "Update LDT_STUDENT_EMAIL to match an existing campus domain."
  exit 1
fi

echo "Campus resolved: $CAMPUS_ID"

# ── Seed accounts ────────────────────────────────────────────────────────────

echo "Seeding accounts..."

psql "$DATABASE_URL" \
  -v student_email="$LDT_STUDENT_EMAIL" \
  -v student_hash="$STUDENT_HASH" \
  -v general_email="$LDT_GENERAL_EMAIL" \
  -v general_hash="$GENERAL_HASH" \
  -v campus_id="$CAMPUS_ID" <<'SQL'

-- LDT STUDENT test account (campus-linked)
INSERT INTO users (id, name, email, password_hash, role, campus_id, email_verified, created_at)
VALUES (
  gen_random_uuid(),
  'LDT Student',
  :'student_email',
  :'student_hash',
  'STUDENT',
  :'campus_id'::uuid,
  true,
  NOW()
)
ON CONFLICT (email) DO UPDATE
  SET password_hash  = EXCLUDED.password_hash,
      role           = 'STUDENT',
      campus_id      = EXCLUDED.campus_id,
      email_verified = true;

-- LDT GENERAL test account (no campus)
INSERT INTO users (id, name, email, password_hash, role, campus_id, email_verified, created_at)
VALUES (
  gen_random_uuid(),
  'LDT General',
  :'general_email',
  :'general_hash',
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
WHERE email IN (:'student_email', :'general_email')
ORDER BY role;

SQL

echo ""
echo "Done. LDT accounts are ready in dev DB."
