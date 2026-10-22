# AssetPulse - Smart Asset Management System (Backend)

Spring Boot 3 / Java 17 REST API for managing laptops, software licences, warranties and other IT assets.

## Features
- **JWT authentication** with roles: `ADMIN`, `MANAGER`, `EMPLOYEE`
- **Asset CRUD** with auto-generated asset tags, search/filter, soft delete (RETIRED)
- **QR codes** (ZXing): PNG per asset; scan endpoints for **check-out / check-in** (and usage logging for allocated assets)
- **Approval workflows**
  - Allocation: Employee -> Manager approves -> Admin approves & picks asset -> allocated
  - Return: Owner -> Admin approves -> asset back in stock
- **Automated alerts** for warranty & licence expiry (daily cron + manual trigger, optional email)
- **Analytics**: dashboard KPIs, idle / underused assets, utilisation by type, and **reassignment recommendations** for open requests

## Quick start
```bash
docker compose up -d          # MySQL (or use your own MySQL and set DB_* env vars)
mvn spring-boot:run           # http://localhost:8080
```
Tables are created automatically (`ddl-auto=update`). On first run a demo dataset is seeded:

| Role     | Email                    | Password       |
|----------|--------------------------|----------------|
| Admin    | admin@assetpulse.com     | Admin@123      |
| Manager  | manager@assetpulse.com   | Manager@123    |
| Employee | alice@assetpulse.com     | Employee@123   |
| Employee | bob@assetpulse.com       | Employee@123   |

Disable demo data with `SEED_DEMO_DATA=false`. **Change JWT_SECRET and the admin password before deploying.**

## Configuration (env vars)
`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (>= 32 chars), `CORS_ORIGINS`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`,
`SEED_DEMO_DATA`, `MAIL_ENABLED`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`.
Other tunables in `application.properties`: `app.alerts.days-before-expiry`, `app.alerts.cron`, `app.analytics.idle-days`.

## API overview
All endpoints except login/register need `Authorization: Bearer <token>`.

| Method | Path | Roles | Purpose |
|---|---|---|---|
| POST | `/api/auth/login` | public | Get JWT |
| POST | `/api/auth/register` | public | Self-register as EMPLOYEE |
| GET | `/api/auth/me` | any | Current user |
| GET/POST/PUT | `/api/users`, `/api/users/{id}` | ADMIN (GET also MANAGER) | User management |
| GET | `/api/users/my-team` | MANAGER, ADMIN | Direct reports |
| GET | `/api/assets?status=&type=&q=` | ADMIN, MANAGER | List/search assets |
| GET | `/api/assets/available`, `/api/assets/mine` | any | Requestable assets / my assets |
| POST/PUT/DELETE | `/api/assets`, `/api/assets/{id}` | ADMIN | Create / update / retire |
| GET | `/api/assets/{id}/qr?size=300` | ADMIN, MANAGER | QR code PNG |
| GET | `/api/assets/{id}/history` | ADMIN, MANAGER | Transaction log |
| GET | `/api/scan/lookup?code=` | any | Resolve scanned QR |
| POST | `/api/scan/check-out`, `/api/scan/check-in` | any | Body `{ "code": "ASSETPULSE:AST-XXXX" }` |
| POST | `/api/requests/allocation`, `/api/requests/return` | any | Raise request |
| GET | `/api/requests/mine` | any | My requests |
| GET | `/api/requests/pending/manager` | MANAGER | Team requests to review |
| GET | `/api/requests/pending/admin`, `/api/requests` | ADMIN | Admin queue / all |
| POST | `/api/requests/{id}/manager-decision` | MANAGER, ADMIN | `{ "approve": true, "comment": "" }` |
| POST | `/api/requests/{id}/admin-decision` | ADMIN | `{ "approve": true, "assetId": 3 }` |
| POST | `/api/requests/{id}/cancel` | owner | Cancel pending request |
| GET | `/api/alerts`, POST `/api/alerts/scan`, POST `/api/alerts/{id}/acknowledge` | ADMIN/MANAGER | Expiry alerts |
| GET | `/api/analytics/dashboard` | ADMIN, MANAGER | KPIs + recent activity |
| GET | `/api/analytics/idle-assets?days=30` | ADMIN, MANAGER | Idle / underused assets |
| GET | `/api/analytics/recommendations?days=30` | ADMIN, MANAGER | Suggested reassignments |
| GET | `/api/analytics/utilisation-by-type` | ADMIN, MANAGER | Utilisation per asset type |

### Example
```bash
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"admin@assetpulse.com","password":"Admin@123"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["token"])')
curl -H "Authorization: Bearer $TOKEN" localhost:8080/api/analytics/recommendations
```

## Project structure
```
src/main/java/com/assetpulse
  config/      SecurityConfig, DataSeeder
  security/    JwtService, JwtAuthFilter
  model/       JPA entities + enums
  repository/  Spring Data repositories
  dto/         Request/response records + Mapper
  service/     Business logic (assets, QR, requests, alerts, analytics)
  controller/  REST controllers
  exception/   ApiException + global handler
```

## Push to GitHub
```bash
git init
git add .
git commit -m "Initial commit: AssetPulse backend"
git branch -M main
git remote add origin https://github.com/<your-username>/assetpulse-backend.git
git push -u origin main
```
