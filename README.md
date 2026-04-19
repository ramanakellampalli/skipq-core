<p align="center">
  <h1 align="center">⚡ SkipQ — Backend API</h1>
  <p align="center">Skip the queue. Order ahead at your campus.</p>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.4-6DB33F?style=for-the-badge&logo=spring&logoColor=white" />
  <img src="https://img.shields.io/badge/PostgreSQL-Neon-00E5CC?style=for-the-badge&logo=postgresql&logoColor=white" />
  <img src="https://img.shields.io/badge/Cloud_Run-GCP-4285F4?style=for-the-badge&logo=googlecloud&logoColor=white" />
</p>

<p align="center">
  <img src="https://img.shields.io/badge/CI%2FCD-GitHub_Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white" />
  <img src="https://img.shields.io/badge/Real--time-Ably-FF5416?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Payments-Razorpay-02042B?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Docs-Swagger_UI-85EA2D?style=for-the-badge&logo=swagger&logoColor=black" />
</p>

---

## What is SkipQ?

SkipQ is a campus food ordering platform. Students order ahead from campus vendors, vendors manage orders in real time, and admins oversee the whole platform. No more standing in queues between classes.

This repo is the **core backend** — a single Spring Boot service that powers all three client apps.

---

## Architecture

```
┌──────────────────────────────────────────────────┐
│   Student App     Vendor Hub      Admin Hub       │
│  (React Native)  (React Native)    (React)        │
└───────────┬──────────────┬─────────────┬──────────┘
            │              │             │
            └──────────────▼─────────────┘
                   Cloud Run · Spring Boot
                      /api/v1/*
            ┌──────────────▼─────────────┐
            │                            │
     ┌──────▼──────┐           ┌─────────▼──────┐
     │  PostgreSQL  │           │      Ably      │
     │   (Neon)    │           │  (real-time)   │
     └─────────────┘           └────────────────┘
```

---

## API at a Glance

> Full interactive docs at `/swagger-ui.html` on any running instance.

| Prefix | Who | Purpose |
|--------|-----|---------|
| `/api/v1/auth` | Public | Register, login, OTP verify, vendor setup |
| `/api/v1/admin` | Admin | Campus + vendor management, platform sync |
| `/api/v1/vendor` | Vendor | Menu CRUD, order status, store profile |
| `/api/v1/student` | Student | Browse vendors, place & track orders |

<details>
<summary><strong>Auth endpoints</strong></summary>

```
POST  /api/v1/auth/register       → create student account, sends OTP
POST  /api/v1/auth/verify-otp     → verify OTP, returns JWT
POST  /api/v1/auth/login          → email + password → JWT
POST  /api/v1/auth/setup-account  → vendor onboarding via invite token
POST  /api/v1/auth/setup-password → admin password setup via invite token
```
</details>

<details>
<summary><strong>Student endpoints</strong></summary>

```
GET   /api/v1/student/sync        → vendors + active order + past orders
GET   /api/v1/student/menu/{id}   → vendor menu
POST  /api/v1/student/orders      → place an order
```
</details>

<details>
<summary><strong>Vendor endpoints</strong></summary>

```
GET    /api/v1/vendor/sync
PATCH  /api/v1/vendor/profile
POST   /api/v1/vendor/menu
PATCH  /api/v1/vendor/menu/{id}
DELETE /api/v1/vendor/menu/{id}
PATCH  /api/v1/vendor/orders/{id}/status
```
</details>

<details>
<summary><strong>Admin endpoints</strong></summary>

```
GET   /api/v1/admin/sync          → stats + campuses + vendors + orders
POST  /api/v1/admin/campuses      → add a new campus
POST  /api/v1/admin/vendors       → create vendor + trigger onboarding
```
</details>

---

## Key Features

### 🏫 Campus-Based Access
Every campus has an affiliated email domain (e.g. `srmap.edu.in`). Students are auto-assigned to their campus on registration. Vendors are tied to a campus. Students only see vendors on their campus — no cross-campus orders.

### 🔐 Auth Flow
- **Students** — register with campus email + password → OTP verification (one-time, on signup only) → JWT login from then on
- **Vendors** — admin creates account → invite email with deep link → vendor sets up password + business details
- **Admins** — seeded directly in DB

### ⚡ Real-Time Orders
Orders are pushed to vendors via **Ably** the instant a student places one. No polling. The vendor app subscribes to `vendor:{vendorId}` on login and receives events in under a second.

---

## Environments

| Environment | Deploy Trigger | Database |
|-------------|---------------|----------|
| **Dev** | Auto on push to `main` | Neon dev branch |
| **Prod** | Manual `workflow_dispatch` | Neon main branch |

All secrets live in **GCP Secret Manager**, injected at runtime via `--set-secrets`.

---

## Local Development

### Prerequisites
- Java 21, Maven
- PostgreSQL or a [Neon](https://neon.tech) account

### Run

```bash
git clone https://github.com/ramanakellampalli/skipq-core.git
cd skipq-core

# Copy and fill in your values
export JWT_SECRET=...
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

# Run in dev mode — enables OTP bypass and test email domain
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## Dev Testing Shortcuts

The `dev` Spring profile (`otp.bypass=true`) enables shortcuts so you can test without real email accounts.

### 🎓 Students

| | |
|---|---|
| **Email** | Any `@test.skipq.dev` address — e.g. `alice@test.skipq.dev` |
| **OTP** | Always `123456` |

### 🏪 Vendors

Create via admin hub or Swagger → login immediately, no email.

| | |
|---|---|
| **Email** | Any address — e.g. `vendor1@test.skipq.dev` |
| **Password** | `Test@1234` |

---

## Project Structure

```
src/main/java/com/skipq/core/
├── admin/          # Platform management: campuses, vendors, stats
├── auth/           # JWT, OTP, registration, login
├── campus/         # Campus entity and email-domain lookup
├── config/         # Security, Swagger, Razorpay, Ably
├── notification/   # Transactional email via Resend
├── order/          # Order placement and lifecycle
├── student/        # Student sync and vendor discovery
└── vendor/         # Vendor profile, menu, orders
```

---

## Database Migrations

Managed by **Flyway**. Files in `src/main/resources/db/migration/`.
Dev and prod run against separate **Neon branches** for full isolation.
