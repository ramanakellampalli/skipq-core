# skipq-core

Backend API for SkipQ — a campus food ordering platform. Built with Spring Boot 3, PostgreSQL, and deployed on Google Cloud Run.

## Stack

- Java 21 / Spring Boot 3.4
- PostgreSQL (Neon)
- Spring Security + JWT
- Ably (real-time order push)
- Resend (transactional email)
- Google Cloud Run (auto-deploy via GitHub Actions)

## Architecture

```
Student App / Vendor App / Admin Hub
            ↓
     Cloud Run (Spring Boot)
            ↓
       PostgreSQL (Neon)
            ↓
    Ably (real-time push to vendor)
```

## API Overview

| Prefix | Role | Description |
|--------|------|-------------|
| `/api/v1/auth` | Public | Login, register, setup password |
| `/api/v1/admin` | ADMIN | Sync dashboard, create vendors |
| `/api/v1/vendor` | VENDOR | Sync, profile, menu, orders |
| `/api/v1/vendors` | STUDENT/VENDOR | Browse open vendors |
| `/api/v1/orders` | STUDENT/VENDOR | Place and view orders |

### Key endpoints

```
POST   /api/v1/auth/login
POST   /api/v1/auth/setup-password

GET    /api/v1/admin/sync              → stats + vendors + orders in one call
POST   /api/v1/admin/vendors           → create vendor + send invite email

GET    /api/v1/vendor/sync             → profile + active orders + past orders + menu
PATCH  /api/v1/vendor/profile
GET    /api/v1/vendor/menu
POST   /api/v1/vendor/menu
PATCH  /api/v1/vendor/menu/{itemId}
DELETE /api/v1/vendor/menu/{itemId}
PATCH  /api/v1/vendor/orders/{id}/status

POST   /api/v1/orders                  → place order (triggers Ably push to vendor)
GET    /api/v1/orders
```

## Real-time

Order events are pushed to vendors via **Ably**. When a student places an order or a vendor updates its status, the backend publishes to the `vendor:{vendorId}` channel with event name `order`. The vendor app subscribes on login.

## Local development

```bash
# Requirements: Java 21, Maven

# Set environment variables
export JWT_SECRET=...
export PROD_DB_URL=jdbc:postgresql://...
export PROD_DB_USERNAME=...
export PROD_DB_PASSWORD=...
export ABLY_API_KEY=...
export RESEND_API_KEY=...
export RESEND_FROM_EMAIL=...
export DEEP_LINK_BASE_URL=...

mvn spring-boot:run
```

## Deploy

Merging to `main` triggers GitHub Actions → builds JAR → builds Docker image → pushes to Artifact Registry → deploys to Cloud Run.

All secrets are stored in GCP Secret Manager and injected at runtime.

## Project structure

```
src/main/java/com/skipq/core/
├── admin/          # Admin endpoints and sync
├── auth/           # JWT, login, setup password
├── config/         # Security config, Ably service
├── menu/           # Menu item CRUD
├── notification/   # Email via Resend
├── order/          # Order placement, status updates
└── vendor/         # Vendor profile, sync
```
