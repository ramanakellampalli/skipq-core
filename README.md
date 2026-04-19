# SkipQ — Backend API

> Skip the queue. Order ahead at your campus.

The core backend powering SkipQ — a campus food ordering platform built for universities. Students order ahead from campus vendors, vendors manage orders in real time, and admins oversee the platform.

Built with **Spring Boot 3**, **PostgreSQL**, and deployed on **Google Cloud Run** with zero-downtime CI/CD.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Runtime | Java 21 / Spring Boot 3.4 |
| Database | PostgreSQL (Neon) + Flyway migrations |
| Auth | Spring Security + JWT |
| Real-time | Ably (push to vendor app) |
| Email | Resend (transactional) |
| Payments | Razorpay |
| Deploy | Google Cloud Run + GitHub Actions |
| Docs | SpringDoc OpenAPI (Swagger UI) |

---

## Architecture

```
┌─────────────────────────────────────────┐
│  Student App  │  Vendor App  │  Admin Hub │
└──────────────┬──────────────┬────────────┘
               │              │
        ┌──────▼──────────────▼──────┐
        │   Cloud Run (Spring Boot)   │
        │   /api/v1/*                 │
        └──────┬──────────────┬───────┘
               │              │
        ┌──────▼──────┐ ┌─────▼──────┐
        │  PostgreSQL  │ │    Ably    │
        │   (Neon)     │ │ (real-time)│
        └─────────────┘ └────────────┘
```

---

## API Reference

Full interactive docs available at `/swagger-ui.html` on any running instance.

| Prefix | Role | Description |
|--------|------|-------------|
| `/api/v1/auth` | Public | Register, login, OTP verify, vendor setup |
| `/api/v1/admin` | ADMIN | Sync dashboard, manage campuses and vendors |
| `/api/v1/vendor` | VENDOR | Sync, menu CRUD, order status updates |
| `/api/v1/student` | STUDENT | Sync, browse vendors, place orders |

### Auth Endpoints

```
POST  /api/v1/auth/register       → create student account, sends OTP to email
POST  /api/v1/auth/verify-otp     → verify email OTP, returns JWT
POST  /api/v1/auth/login          → email + password, returns JWT
POST  /api/v1/auth/setup-account  → vendor onboarding via invite token
POST  /api/v1/auth/setup-password → admin password setup via invite token
```

### Student Flow

```
POST  /api/v1/student/orders      → place order
GET   /api/v1/student/sync        → vendors + active order + past orders
GET   /api/v1/student/menu/{id}   → vendor menu
```

### Vendor Flow

```
GET   /api/v1/vendor/sync         → profile + orders + menu
PATCH /api/v1/vendor/profile
POST  /api/v1/vendor/menu
PATCH /api/v1/vendor/menu/{id}
DELETE /api/v1/vendor/menu/{id}
PATCH /api/v1/vendor/orders/{id}/status
```

### Admin Flow

```
GET   /api/v1/admin/sync          → stats + campuses + vendors + orders
POST  /api/v1/admin/campuses      → add a new campus
POST  /api/v1/admin/vendors       → create vendor + trigger onboarding
```

---

## Campus-Based Access

SkipQ is multi-campus. Each campus has an associated email domain (e.g. `srmap.edu.in`). Students are automatically assigned to a campus on registration based on their email. Vendors are tied to a campus at creation time. Students can only order from vendors on their campus.

---

## Real-Time Orders

When a student places an order, the backend publishes to the `vendor:{vendorId}` Ably channel. The vendor app receives the order instantly without polling. Status updates from the vendor propagate back the same way.

---

## Environments

| Environment | Trigger | Database |
|-------------|---------|---------|
| **Dev** | Auto-deploy on push to `main` | Neon dev branch |
| **Prod** | Manual `workflow_dispatch` | Neon main branch |

All secrets are stored in **GCP Secret Manager** and injected at runtime via `--set-secrets`.

---

## Local Development

### Prerequisites

- Java 21
- Maven
- PostgreSQL (or a Neon account)

### Run

```bash
# Clone
git clone https://github.com/ramanakellampalli/skipq-core.git
cd skipq-core

# Set environment variables
export JWT_SECRET=your-secret
export PROD_DB_URL=jdbc:postgresql://...
export PROD_DB_USERNAME=...
export PROD_DB_PASSWORD=...
export ABLY_API_KEY=...
export RESEND_API_KEY=...
export RESEND_FROM_EMAIL=noreply@yourdomain.com
export RAZOR_TEST_ID=...
export RAZOR_TEST_SECRET=...
export RAZOR_WEBHOOK_SECRET=...
export DEEP_LINK_BASE_URL=skipq://

# Run with dev profile (enables OTP bypass + test domain)
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## Dev Testing

The `dev` Spring profile enables testing shortcuts so you don't need real email accounts.

### Students

| What | Value |
|------|-------|
| Email domain | Any `@test.skipq.dev` address (e.g. `alice@test.skipq.dev`) |
| OTP code | Always `123456` |

### Vendors

Vendors created via `POST /admin/vendors` when running in dev profile are immediately usable — no invite email is sent.

| What | Value |
|------|-------|
| Email | Any address (e.g. `vendor1@test.skipq.dev`) |
| Password | `Test@1234` |
| Business details | Pre-filled with dummy values |

Create via admin hub or Swagger, then log straight into the vendor app.

### Dev Profile Config (`application-dev.yml`)

```yaml
otp:
  bypass: true
  fixed-code: "123456"
  allowed-test-domain: test.skipq.dev
```

---

## Database Migrations

Managed by **Flyway**. Migration files live in `src/main/resources/db/migration/`.

- Dev and prod run against separate Neon branches
- `baseline-version=2` — V1 and V2 were applied manually before Flyway was introduced; only V3+ auto-run

---

## Project Structure

```
src/main/java/com/skipq/core/
├── admin/          # Admin sync, campus and vendor management
├── auth/           # JWT, registration, OTP, login
├── campus/         # Campus entity and repository
├── config/         # Security, Swagger, Razorpay, Ably
├── notification/   # Email via Resend
├── order/          # Order placement and status
├── student/        # Student sync and vendor browsing
└── vendor/         # Vendor profile, menu, orders
```
