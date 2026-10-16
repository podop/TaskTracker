# Development Plan: Ticket list filter by assignee
Plan: Ticket list filter by assignee · Status: approved (2026-10-15 by Yurii Pidopryhora, as written incl. assumptions A1–A7) · Spec: `specs/2026-10-15-ticket-list-assignee-filter.md` · Base: feat/sdd-engineering-trial@b62a547
Packages: backend, docs · Context pack: `docs/plans/2026-10-15-ticket-list-assignee-filter.context.md`
Execution: single-agent (chosen by the user)

## Goal
`GET /api/tickets` accepts an optional `assignee` query parameter (mock user id or username, resolved like assignment) and returns only that user's tickets, ANDed with `status`, ordered by `updatedAt` desc. Unknown user → 400 problem details. Documented in `docs/api.md` and `/v3/api-docs`.

## Out of scope
- Anything under `frontend/`; DB schema / Flyway migrations; manager endpoint; pagination; unassigned or multi-value filter (spec §3).

## Decisions
- Resolve the filter inside `TicketService` via `MockUserService.findByIdOrUsername`, mirroring `assign` — [F3] [F4], spec D9
- Query the repository with the resolved user id only, never the raw parameter — [F5] [F6]; engineering-paved-path:security § A05
- No index on `assignee_id` (D10 weighed): schema changes are out of scope — [K8] [F11]
- Replace `TicketService.list(TicketStatus)` by `list(TicketStatus, String)`; one caller — [F2]

## Recommendations not adopted
- Index `tickets(assignee_id)` via a new Flyway migration — excluded by spec §3; revisit if latency matters.
- Project-style problem details for invalid `status` — rejected in spec D7.

## Steps
### S1 — Repository queries and service filter [backend]
- Files: M `backend/src/main/java/com/tasktracker/repository/TicketRepository.java` — 2 derived queries; M `.../service/TicketService.java` — filter logic; M `backend/src/test/java/com/tasktracker/service/TicketServiceTest.java` — unit cases
- Change: add `findAllByAssigneeIdOrderByUpdatedAtDesc(String)` and `findAllByStatusAndAssigneeIdOrderByUpdatedAtDesc(TicketStatus, String)`. `list(TicketStatus statusFilter, String assigneeFilter)`: null or `isBlank()` → today's two branches unchanged; else resolve, `orElseThrow(new UnknownUserException("Unknown mock user: " + assigneeFilter))` with the raw value, before any repository call; then pick the query by `statusFilter == null`.
- Rules: [K1] [F3] [F4] [F5]; engineering-paved-path:security § A05 (parameterised derived query)
- Covers: FR2, FR3, FR4, FR5, FR6, FR7 (unit level; ACs closed in S2)
- Tests: M `TicketServiceTest` (unit, mock repo) — null/""/"  " → existing repo methods; `alice`, `u-001`, ` ALICE ` → `findAllByAssigneeId…("u-001")`; + status → combined query; `ghost` → `UnknownUserException`, message `Unknown mock user: ghost`, `verifyNoInteractions(tickets)`
- Done when: `mvn -B -ntp -f backend/pom.xml test -Dtest=TicketServiceTest` passes — source: [V1]

### S2 — Controller parameter and API integration tests [backend]
- Files: M `.../controller/TicketController.java` — new param; M `backend/src/test/java/com/tasktracker/integration/TicketApiIT.java` — new tests + helpers
- Change: `list(...)` gains `@RequestParam(value = "assignee", required = false) String assignee`, passed to `tickets.list(status, assignee)`. No trimming, converter or validation in the controller (keeps status-binding precedence, EC7, and repeated-value behaviour, A1).
- Rules: [K2] [K4] [F7] [F8] [F12]; engineering-paved-path:security § A05 (echo only inside JSON body)
- Covers: AC1–AC13
- Tests: A methods in `TicketApiIT` (integration): seed helper (T1, T2 → alice, T3 → bob, T4 unassigned) and `assign(id, user)` helper mirroring `changeStatus`; one test per AC1–AC12, exact sets via `$[*].id` containsInAnyOrder / `$.length()`; AC6 also asserts `application/problem+json` and that no response header contains `ghost`; AC7 compares bodies with the unfiltered call; AC8 orders T1 last-updated; AC12 uses `.param("assignee", "alice", "bob")`. Existing tests (lines 69-80, 167-176) untouched (AC13).
- Done when: `mvn -B -ntp -f backend/pom.xml verify -Dit.test=TicketApiIT` passes — source: [V2]

### S3 — OpenAPI description and API reference [backend, docs]
- Files: M `.../controller/TicketController.java` — springdoc annotations; M `TicketApiIT.java` — api-docs test; M `docs/api.md` — List tickets + error table
- Change: `@Parameter` on `assignee` (id or username, trimmed, case-insensitive, blank = no filter, one value). `@ApiResponses` on `list`: `200` (array of `TicketResponse`, must stay documented) and `400` (`application/problem+json`, unknown mock user / invalid status). `docs/api.md` "List tickets": examples `?assignee=alice`, `?assignee=u-001&status=IN_PROGRESS`, AND rule, 400 case; error table row "Unknown mock user in `assignee` query parameter | 400 | `Unknown mock user: <value>`".
- Rules: [K5] [K4] [F10]
- Covers: AC14, FR8
- Tests: A `TicketApiIT` test (integration, A2) — `GET /v3/api-docs`: `/api/tickets` get has query param `assignee`, not required, `schema.type` `string`; responses contain `200` and `400`. `docs/api.md` checked manually.
- Done when: `mvn -B -ntp -f backend/pom.xml verify -Dit.test=TicketApiIT` passes and `docs/api.md` shows the example and 400 case — source: [V2]

## Execution
- Mode: single-agent — one package, 5 files, additive contract, no data change.
- Waves: W1: S1, S2, S3 — backend + docs.

## Contracts & migrations
- `GET /api/tickets` gains optional `assignee` (additive); only consumer `frontend/src/api/ticketApi.ts` unaffected [F15]. No migration.

## Verification
- Gate (end of W1): `mvn -B -ntp -f backend/pom.xml verify` — source: [V3]. Requires Docker [K7].
- `git diff --stat -- frontend/ backend/src/main/resources/db/` is empty (spec §3).
- Manual: AC14 `docs/api.md` review; optional Swagger UI look.
- Review risk: high — shared API contract + user input echoed in error body.

## Assumptions
- A1 Repeated `assignee` (EC10/AC12): error detail unspecified. Chosen: no extra code; Spring joins repeated values with `,` [F12] → `400 Unknown mock user: alice,bob`. Why: no invented message, reuses FR5 path; AC12 test pins it. Revisit: explicit `getParameterValues` check if the spec wants another detail.
- A2 AC14 is "manual": added an automated `/v3/api-docs` IT as well. Why: cheap, regression-proof. Revisit: drop if unwanted.
- A3 AC13 "same as before the change": read as existing list tests unchanged and green plus AC7. Revisit with a concrete fixture in the spec.
- A4 D10 index: not added (spec §3 forbids schema change). Revisit via new migration V2.
- A5 Error `<value>` is the raw, untrimmed parameter, like `assign` [F3] and FR5 "as received".
- A6 Spec `Status: ready-for-planning` with D1–D10 user-accepted treated as approved (per caller).
- A7 Frontend test suites are not gates: no frontend file changes (checked by the diff above).

## Spec issues (for spec-creator; none blocking)
- SI1 Stale refs: `docs/api.md:56-63`→46-53, `73-88`→63-77, `103-109`→93-99, `125`→126; `application-it.yml:13-19`→14-20.
- SI2 AC13 "same tickets as before the change" is not observable after the change; restate as a fixture.
- SI3 FR8 requires OpenAPI to document accepted values and the 400 error; AC14 checks only the parameter — extend AC14.
- SI4 EC10/AC12: no `detail` for repeated values; FR5 "value as received" is ambiguous for multi-value (A1).
- SI5 NFR2 "never in a response header" and §6 "validation before any ticket is read" have no AC (plan tests them in S2/S1).
- SI6 AC11 under MockMvc: the type-mismatch 400 has no body, so the `detail` check passes vacuously.
