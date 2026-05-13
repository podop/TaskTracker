# Task Tracker Project Tasks

## Project Scope

Build a Jira-inspired task tracker with:

- Backend: Spring Boot 3+, Maven
- Frontend: React
- Database: PostgreSQL
- Deployment: Docker Compose
- Test cycle: backend unit tests, backend integration tests, UI tests
- Documentation: API docs, local setup, test/deploy instructions

## Domain Workflow

Ticket statuses:

- `TODO`
- `IN_PROGRESS`
- `DONE_CANDIDATE`
- `DONE`

Main flow:

1. User creates a ticket.
2. User views ticket list.
3. User assigns ticket to a mocked user.
4. User moves ticket from `TODO` to `IN_PROGRESS`.
5. User moves ticket to `DONE_CANDIDATE` and waits for manager approval.
6. Manager views all `DONE_CANDIDATE` tickets.
7. Manager marks an approved candidate as `DONE`.

## Epic 1: Project Bootstrap

### TT-001: Create Spring Boot Backend Skeleton

Description:
Set up a Spring Boot 3+ Maven backend service.

Acceptance criteria:

- Maven project builds successfully.
- Spring Boot application starts locally.
- Health endpoint is available.
- Base package structure is created for controller, service, repository, model, DTO, and config layers.

Technical notes:

- Use Java 17 or newer.
- Add Spring Web, Spring Data JPA, Validation, PostgreSQL Driver, Flyway or Liquibase, and Testcontainers.

### TT-002: Create React UI Skeleton

Description:
Set up a React frontend application.

Acceptance criteria:

- UI starts locally.
- Base app layout is available.
- Routing structure is prepared for ticket list and ticket detail/create flows.
- API client layer is created.

Technical notes:

- Use Vite or equivalent modern React tooling.
- Use a Jira-inspired work management layout: left navigation, top toolbar, issue list/board area, status labels, assignee display.

### TT-003: Add Docker Compose Local Environment

Description:
Create Docker Compose configuration for backend, frontend, and PostgreSQL.

Acceptance criteria:

- `docker compose up` starts the full app stack.
- PostgreSQL is reachable by the backend.
- Environment variables are documented.
- Database data is persisted in a named volume.

## Epic 2: Ticket Backend API

### TT-004: Create Ticket Persistence Model

Description:
Implement ticket database schema and JPA entity.

Acceptance criteria:

- Ticket table exists in PostgreSQL.
- Ticket fields include id, title, description, status, assignee, createdAt, updatedAt.
- Status is stored as a constrained enum or equivalent safe representation.
- Migration is versioned.

### TT-005: Create Ticket Endpoint

Description:
Implement endpoint for creating a ticket.

Endpoint:

```http
POST /api/tickets
```

Acceptance criteria:

- Request accepts title and optional description.
- New ticket is created with `TODO` status.
- Response returns created ticket data.
- Invalid title returns validation error.
- Unit and integration tests cover success and validation failure.

### TT-006: Get Ticket List Endpoint

Description:
Implement endpoint for retrieving tickets.

Endpoint:

```http
GET /api/tickets
```

Acceptance criteria:

- Response returns all tickets ordered by creation date or update date.
- Response includes id, title, description, status, assignee, createdAt, updatedAt.
- Optional status filter is supported if it fits the implementation scope.
- Integration test verifies persisted tickets are returned.

### TT-007: Assign Ticket To Mock User

Description:
Implement endpoint for assigning a ticket to a mocked user.

Endpoint:

```http
PATCH /api/tickets/{ticketId}/assignee
```

Acceptance criteria:

- Request accepts mocked user id or username.
- Backend validates that the mock user exists in the predefined mock user list.
- Ticket assignee is updated.
- Unknown ticket returns 404.
- Unknown mock user returns validation error.
- Unit and integration tests cover success and failure cases.

### TT-008: Move Ticket To In Progress

Description:
Implement endpoint for changing ticket status from `TODO` to `IN_PROGRESS`.

Endpoint:

```http
PATCH /api/tickets/{ticketId}/status
```

Acceptance criteria:

- Request supports moving a ticket to `IN_PROGRESS`.
- Only valid status transitions are allowed.
- `TODO -> IN_PROGRESS` succeeds.
- Invalid transition returns a clear error.
- Unit tests cover transition rules.
- Integration tests cover API behavior.

### TT-009: Submit Ticket For Manager Approval

Description:
Allow a user to move a ticket to `DONE_CANDIDATE`.

Endpoint:

```http
PATCH /api/tickets/{ticketId}/status
```

Acceptance criteria:

- `IN_PROGRESS -> DONE_CANDIDATE` succeeds.
- Ticket is not marked as final `DONE` directly by a regular user.
- Invalid direct transitions to `DONE` are rejected.
- Tests cover approval workflow rules.

## Epic 3: Manager Approval Backend API

### TT-010: Get Done Candidate Tickets

Description:
Implement manager endpoint for retrieving tickets waiting for approval.

Endpoint:

```http
GET /api/manager/tickets/done-candidates
```

Acceptance criteria:

- Response returns only tickets with `DONE_CANDIDATE` status.
- Response includes enough data for manager review.
- Integration test verifies filtering.

### TT-011: Mark Ticket As Done

Description:
Implement manager endpoint for approving a done candidate.

Endpoint:

```http
PATCH /api/manager/tickets/{ticketId}/mark-done
```

Acceptance criteria:

- `DONE_CANDIDATE -> DONE` succeeds.
- Tickets in other statuses cannot be marked as `DONE` through this endpoint.
- Unknown ticket returns 404.
- Integration tests cover success and invalid status cases.

## Epic 4: React UI

### TT-012: Ticket List UI

Description:
Build Jira-inspired ticket list view.

Acceptance criteria:

- User can see all tickets.
- Status and assignee are visible.
- Empty state is handled.
- Loading and error states are handled.
- UI test verifies tickets render from mocked API data.

### TT-013: Create Ticket UI

Description:
Build create ticket flow.

Acceptance criteria:

- User can enter title and description.
- Submit creates a ticket through backend API.
- Validation errors are displayed.
- After creation, ticket list updates or user is redirected to the list.
- UI test covers successful creation.

### TT-014: Assign Ticket UI

Description:
Allow assigning a ticket to a mock user.

Acceptance criteria:

- User can select a mock user.
- Selected assignee is saved through backend API.
- UI reflects updated assignee.
- Error state is shown if assignment fails.

### TT-015: Ticket Status Transition UI

Description:
Allow changing ticket status through the expected workflow.

Acceptance criteria:

- User can move `TODO` ticket to `IN_PROGRESS`.
- User can submit `IN_PROGRESS` ticket as `DONE_CANDIDATE`.
- UI does not expose invalid status actions.
- UI test covers the basic transition flow.

### TT-016: Manager Approval UI

Description:
Build manager view for reviewing done candidates.

Acceptance criteria:

- Manager can open a list of `DONE_CANDIDATE` tickets.
- Manager can mark a candidate as `DONE`.
- Approved ticket disappears from candidate list or updates status immediately.
- UI test covers manager approval flow.

## Epic 5: Documentation

### TT-017: API Documentation

Description:
Document all backend endpoints.

Acceptance criteria:

- Request and response examples are documented.
- Validation and error responses are documented.
- Status transition rules are documented.
- Manager endpoints are clearly separated from regular user endpoints.

Suggested options:

- OpenAPI/Swagger generated docs.
- Markdown API reference in `docs/api.md`.

### TT-018: Local Development Documentation

Description:
Document how to run the project locally.

Acceptance criteria:

- Backend setup is documented.
- Frontend setup is documented.
- Docker Compose setup is documented.
- Database migration behavior is documented.
- Required Java, Node, Maven, and Docker versions are listed.

### TT-019: Test Documentation

Description:
Document full test cycle.

Acceptance criteria:

- Backend unit test command is documented.
- Backend integration test command is documented.
- UI test command is documented.
- Full test cycle command or sequence is documented.
- CI-ready expectations are described.

## Epic 6: Test Coverage And Quality

### TT-020: Backend Unit Tests

Description:
Add unit tests for backend business logic.

Acceptance criteria:

- Ticket creation logic is covered.
- Assignment logic is covered.
- Status transition rules are covered.
- Manager approval rules are covered.

### TT-021: Backend Integration Tests

Description:
Add integration tests for REST API and PostgreSQL persistence.

Acceptance criteria:

- Testcontainers PostgreSQL is used.
- Ticket create/list/assign/status endpoints are covered.
- Manager candidate list and mark-done endpoints are covered.
- Tests run from Maven.

### TT-022: UI Tests

Description:
Add UI tests for critical React flows.

Acceptance criteria:

- Ticket list rendering is covered.
- Ticket creation is covered.
- Assignment flow is covered.
- Status transition flow is covered.
- Manager approval flow is covered.

Suggested tools:

- React Testing Library for component tests.
- Playwright or Cypress for end-to-end tests.

### TT-023: Full Test Cycle Script

Description:
Create a repeatable command for the full test cycle.

Acceptance criteria:

- Command runs backend unit tests.
- Command runs backend integration tests.
- Command runs UI tests.
- Documentation references the command.
- Command exits non-zero on any failure.

## Suggested Milestones

### Milestone 1: Backend MVP

- TT-001
- TT-004
- TT-005
- TT-006
- TT-007
- TT-008
- TT-009
- TT-010
- TT-011
- TT-020
- TT-021

### Milestone 2: UI MVP

- TT-002
- TT-012
- TT-013
- TT-014
- TT-015
- TT-016
- TT-022

### Milestone 3: Delivery Readiness

- TT-003
- TT-017
- TT-018
- TT-019
- TT-023

