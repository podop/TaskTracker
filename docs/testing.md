# Testing

The project has three layers of tests:

1. **Backend unit tests** — fast JUnit/Mockito tests for the business logic in
   `TicketService` and `TicketStatus`. No Spring context, no database.
2. **Backend integration tests** — `@SpringBootTest` with `MockMvc` running
   against a real PostgreSQL container via
   [Testcontainers](https://java.testcontainers.org/).
3. **Frontend tests**
   - Vitest + React Testing Library for component and page logic.
   - Playwright for end-to-end flows in a real browser, with the network
     mocked at the route level.

## Running each layer

| Layer                        | Command                                       | Working directory |
|------------------------------|-----------------------------------------------|-------------------|
| Backend unit tests           | `mvn test`                                    | `backend/`        |
| Backend integration tests    | `mvn verify`                                  | `backend/`        |
| Frontend unit/component tests| `npm run test`                                | `frontend/`       |
| Frontend e2e (Playwright)    | `npm run test:e2e`                            | `frontend/`       |
| Install Playwright browser   | `npm run test:e2e:install`                    | `frontend/`       |

> Playwright auto-starts the Vite dev server defined in
> `frontend/playwright.config.ts`. No backend is required because the e2e tests
> mock the API at the network layer.

## Full test cycle

The repeatable command for CI and local "everything" runs is:

```bash
./scripts/run-all-tests.sh
```

It executes, in order:

1. `mvn -B -ntp -f backend/pom.xml verify` (runs both unit and integration tests)
2. `npm --prefix frontend run test`
3. `npm --prefix frontend run test:e2e`

The script uses `set -euo pipefail`, so any failure aborts the run and the
overall exit code is non-zero. This matches what we want from CI.

## CI expectations

A pipeline that runs `./scripts/run-all-tests.sh` is sufficient. To make CI
green from a clean machine, install (or cache) the following first:

- JDK 21
- Maven 3.9+
- Node 20+
- Docker (required by Testcontainers and Playwright system dependencies)
- Playwright browser binaries: `npm --prefix frontend run test:e2e:install`

The Maven build pins the Docker API version (`-Dapi.version=1.43` configured in
`pom.xml`) so that Testcontainers works against Docker engines whose minimum
supported API version is `1.40`+. Override with `-Ddocker.api.version=...` if a
newer or older value is needed for your environment.
