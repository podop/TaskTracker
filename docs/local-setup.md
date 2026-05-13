# Local Setup

## Prerequisites

| Tool           | Minimum version | Notes                          |
|----------------|-----------------|--------------------------------|
| Java JDK       | 21              | Required by Spring Boot 3.3    |
| Apache Maven   | 3.9             | Used for backend build/tests   |
| Node.js        | 20              | LTS; 22 verified locally       |
| npm            | 10              | Bundled with Node              |
| Docker         | 24              | Engine API ≥ 1.40              |
| Docker Compose | v2              | Bundled with modern Docker     |

## Repository layout

```
backend/           Spring Boot 3.3 Maven module
frontend/          Vite + React + TypeScript app
docker/            Dockerfiles + nginx config
docker-compose.yml Full local stack
scripts/           CI helpers (e.g. run-all-tests.sh)
docs/              Project documentation
```

## Running with Docker Compose (recommended)

```bash
cp .env.example .env       # optional overrides
docker compose up --build
```

This starts three services:

- `postgres` — PostgreSQL 16, data persisted in the named volume `tasktracker_pgdata`.
- `backend`  — Spring Boot on `http://localhost:8080` (Swagger UI at `/swagger-ui.html`).
- `frontend` — nginx serving the built React app on `http://localhost:5173`,
  proxying `/api` to the backend.

The backend waits for the database health check before starting, and the
frontend waits for the backend health check. Stop everything with
`docker compose down`. To wipe the database, add `-v`.

## Running services individually

### Postgres

```bash
docker compose up -d postgres
```

### Backend

```bash
cd backend
mvn spring-boot:run
```

The backend reads connection details from these environment variables (with
sensible defaults that match `.env.example`):

- `DB_URL` (default `jdbc:postgresql://localhost:5432/tasktracker`)
- `DB_USERNAME` (default `tasktracker`)
- `DB_PASSWORD` (default `tasktracker`)
- `SERVER_PORT` (default `8080`)
- `CORS_ALLOWED_ORIGINS` (default `http://localhost:5173`)

Flyway runs on startup and applies migrations from
`backend/src/main/resources/db/migration`.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

The Vite dev server runs on `http://localhost:5173` and proxies `/api`
requests to `http://localhost:8080` by default. Override with
`VITE_API_TARGET=http://hostname:port npm run dev`.

## Database migrations

- Tool: **Flyway** (configured via `spring.flyway.enabled=true`).
- Location: `backend/src/main/resources/db/migration` — each file is named
  `V<version>__<description>.sql`.
- The first migration (`V1__create_tickets.sql`) creates the `tickets` table
  and the `ck_tickets_status` check constraint.
- Migrations run automatically on backend startup. They also run inside the
  Testcontainers PostgreSQL used by integration tests.

To create a new migration, add a new file with the next version number — never
edit an existing migration that has already shipped.

## Useful URLs

| URL                                              | Purpose                |
|--------------------------------------------------|------------------------|
| `http://localhost:8080/actuator/health`          | Backend health probe   |
| `http://localhost:8080/swagger-ui.html`          | OpenAPI / Swagger UI   |
| `http://localhost:8080/v3/api-docs`              | OpenAPI JSON           |
| `http://localhost:5173/`                         | Frontend home          |
| `http://localhost:5173/tickets`                  | Ticket list view       |
| `http://localhost:5173/manager`                  | Manager approvals view |
