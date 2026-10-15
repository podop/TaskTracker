# Context pack: Ticket list filter by assignee
No `CLAUDE.md`, `AGENTS.md`, `CONTRIBUTING.md`, ADRs, INSIGHTS or CI workflows exist in the repo; rules come from `docs/` and the code. Java paths are under `backend/src/main/java/com/tasktracker/` unless they start with `backend/`.

## Repo rules and notes that apply
- [K1] `docs/testing.md:5-6` — "Backend unit tests — fast JUnit/Mockito tests for the business logic in `TicketService` and `TicketStatus`. No Spring context, no database." → S1 tests stay plain Mockito.
- [K2] `docs/testing.md:7-9` — "Backend integration tests — `@SpringBootTest` with `MockMvc` running against a real PostgreSQL container via Testcontainers." → S2/S3 tests go into `TicketApiIT`.
- [K3] `backend/pom.xml:120-122` — failsafe `<include>**/*IT.java</include>` → integration tests must live in an `*IT` class.
- [K4] `docs/api.md:9-10` — "Errors follow RFC 7807 Problem Details (`application/problem+json`) with `status` and `detail` fields." → unknown user goes through the existing handler.
- [K5] `docs/api.md:3-7` — "Live, machine-readable docs are served by springdoc-openapi" (`/v3/api-docs`) → S3 annotations.
- [K7] `docs/testing.md:54` — "Docker (required by Testcontainers ...)"; `:57-60` Docker API pinned via `docker.api.version`.
- [K8] spec §3 — "No file under `frontend/` is changed" / "No persisted data or schema change is made."

## Verified facts
- [F1] `controller/TicketController.java:44-47` — `list(@RequestParam(value = "status", required = false) TicketStatus status)`.
- [F2] `service/TicketService.java:34-39` — `list(TicketStatus)`; its only caller is `TicketController.java:46`.
- [F3] `service/TicketService.java:47-50` — `assign` resolves user first, throws `UnknownUserException("Unknown mock user: " + rawValue)`.
- [F4] `service/MockUserService.java:23-31` — `findByIdOrUsername`: null → empty; trims; case-insensitive id or username; first match.
- [F5] `repository/TicketRepository.java:14-16` — Spring Data derived queries `findAllBy…OrderByUpdatedAtDesc`.
- [F6] `model/Ticket.java:33-34` — field `assigneeId`, column `assignee_id`, nullable (stores user id).
- [F7] `exception/GlobalExceptionHandler.java:25-28` — `UnknownUserException` → `ProblemDetail` 400; no handler for `MethodArgumentTypeMismatchException` (15-41) → invalid `status` gets Spring's default 400 (empty body under MockMvc).
- [F8] `backend/src/test/java/com/tasktracker/integration/TicketApiIT.java:41-44` — `@BeforeEach` deletes all tickets; helpers `createTicket` 183-191, `changeStatus` 193-197; no assign helper; existing list tests 69-80 and 167-176.
- [F9] `backend/src/test/resources/application-it.yml:14-20` — only `u-001 alice`, `u-002 bob`.
- [F10] `config/OpenApiConfig.java` — only `OpenAPI` info bean; no `io.swagger.v3.oas.annotations` used yet; springdoc 2.6.0 (`backend/pom.xml:22,61-65`) brings them transitively.
- [F11] `backend/src/main/resources/db/migration/V1__create_tickets.sql:12-13` — indexes on `status` and `updated_at` only.
- [F12] Spring MVC (Boot 3.3.5, `backend/pom.xml:10`) binds a repeated `@RequestParam String` as the values joined with `,` — framework behaviour, not exercised in the repo yet; AC12 test pins it.
- [F13] `backend/src/test/java/com/tasktracker/service/TicketServiceTest.java:35-48` — mocked `TicketRepository`, real `MockUserService` with only alice.
- [F15] `frontend/src/api/ticketApi.ts:10-15` — sends `status` only.

## Mirrors
- `TicketService.assign` — user resolution + error message pattern.
- `TicketRepository` — derived-query naming.
- `TicketApiIT.changeStatus` — shape for a new `assign(id, user)` helper; `filtersListByStatus` — list assertion style.

## Skill map
| Step | Files (glob) | Skills | Key rules |
|---|---|---|---|
| S1 | `repository/TicketRepository.java`, `service/TicketService.java`, `service/TicketServiceTest.java` | engineering-paved-path:security | A05: user input never concatenated into queries; only resolved id reaches the derived query |
| S2 | `controller/TicketController.java`, `integration/TicketApiIT.java` | engineering-paved-path:security | A05 XSS: echoed value only inside JSON problem body (NFR2); query values are strings, validate before use |
| S3 | `controller/TicketController.java`, `integration/TicketApiIT.java`, `docs/api.md` | none matched | — |
No TS/React/Fastify/Drizzle/Zod files are touched; `engineering-paved-path:onion-architecture` targets TypeScript backends and does not apply (repo uses Spring `controller/service/repository` packages).

## Verification sources
- [V1] `mvn -B -ntp -f backend/pom.xml test -Dtest=TicketServiceTest` — `docs/testing.md:19` (`mvn test` in `backend/`) + Surefire `-Dtest` filter.
- [V2] `mvn -B -ntp -f backend/pom.xml verify -Dit.test=TicketApiIT` — `docs/testing.md:20,39` + Failsafe `-Dit.test` filter (`backend/pom.xml:108-127`).
- [V3] `mvn -B -ntp -f backend/pom.xml verify` — `docs/testing.md:39`, `scripts/run-all-tests.sh:15`, `Makefile:47`.

## Risks
- Explicit `@ApiResponses` can drop springdoc's inferred `200` — declare `200` explicitly; S3 test asserts it.
- Assignee resolution placed in a converter/argument resolver would run during binding and break EC7 precedence — keep it in the service.
- `updatedAt` ordering in ITs depends on mutation order — make T1 the last ticket mutated for AC8.
- Testcontainers needs a running Docker daemon [K7].

## Notes for reviewers
- Architecture: change stays in existing controller → service → repository layers; no new types, no migration; frontend untouched.
- Security: parameter is resolved against a config list before use; only a known user id reaches SQL (parameterised); raw value echoed only in `application/problem+json` `detail` (NFR2).
