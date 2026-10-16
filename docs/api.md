# Task Tracker API

The backend exposes a small REST API on port `8080`. Live, machine-readable docs
are served by springdoc-openapi:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

Errors follow [RFC 7807 Problem Details](https://datatracker.ietf.org/doc/html/rfc7807)
(`application/problem+json`) with `status` and `detail` fields.

## Ticket statuses

| Value             | Meaning                              |
|-------------------|--------------------------------------|
| `TODO`            | Newly created, no work started       |
| `IN_PROGRESS`     | Assignee is actively working on it   |
| `DONE_CANDIDATE`  | Submitted for manager approval       |
| `DONE`            | Approved by a manager                |

Allowed transitions (user-initiated):

- `TODO → IN_PROGRESS`
- `IN_PROGRESS → DONE_CANDIDATE`

Only a manager can perform the final `DONE_CANDIDATE → DONE` transition through
the dedicated manager endpoint.

## User endpoints

### Create ticket

```http
POST /api/tickets
Content-Type: application/json

{
  "title": "Fix login bug",
  "description": "users locked out"
}
```

- `201 Created` with the new ticket in the response body and `Location` header.
- `400 Bad Request` if `title` is blank or longer than 200 characters.

### List tickets

```http
GET /api/tickets
GET /api/tickets?status=IN_PROGRESS
GET /api/tickets?assignee=alice
GET /api/tickets?assignee=u-001&status=IN_PROGRESS
```

Returns an array of tickets ordered by `updatedAt` descending.

Query parameters (both optional):

- `status` - only tickets in this status.
- `assignee` - only tickets assigned to this mock user. Accepts the mock user id
  (`u-001`) or the username (`alice`), resolved like the assignment endpoint:
  surrounding whitespace is ignored and matching is case-insensitive. An empty
  or whitespace-only value means no filter. Only one value is supported per
  request.

When both are given, a ticket must match both (logical AND). Unassigned tickets
are never returned when `assignee` is applied.

- `200 OK` with the matching tickets (an empty array when nothing matches).
- `400 Bad Request` if `assignee` is not a known mock user
  (`Unknown mock user: <value>`, problem details). An invalid `status` also
  returns `400` and is reported first.

### Get a single ticket

```http
GET /api/tickets/{id}
```

`404 Not Found` if the id is unknown.

### Assign a ticket

```http
PATCH /api/tickets/{id}/assignee
Content-Type: application/json

{ "assignee": "alice" }
```

The `assignee` field accepts either the mock user id (`u-001`) or the username
(`alice`).

- `200 OK` with the updated ticket.
- `400 Bad Request` if the mock user is unknown.
- `404 Not Found` if the ticket id is unknown.

### Change ticket status (user)

```http
PATCH /api/tickets/{id}/status
Content-Type: application/json

{ "status": "IN_PROGRESS" }
```

- `200 OK` on a valid transition.
- `409 Conflict` for illegal transitions or any user-initiated attempt to move
  directly to `DONE`.
- `404 Not Found` if the ticket id is unknown.

### List mock users

```http
GET /api/users
```

Returns the static list of users that tickets can be assigned to.

## Manager endpoints

### List done candidates

```http
GET /api/manager/tickets/done-candidates
```

Returns tickets with status `DONE_CANDIDATE`, ordered by `updatedAt` desc.

### Mark a ticket as done

```http
PATCH /api/manager/tickets/{id}/mark-done
```

- `200 OK` with the ticket now in `DONE`.
- `409 Conflict` if the ticket is not in `DONE_CANDIDATE`.
- `404 Not Found` if the ticket id is unknown.

## Validation and error responses

| Scenario                                | Status | `detail`                                                       |
|-----------------------------------------|--------|----------------------------------------------------------------|
| Missing or empty `title`                | 400    | `title: must not be blank`                                     |
| Unknown mock user in `assignee` body    | 400    | `Unknown mock user: <value>`                                   |
| Unknown mock user in `assignee` query parameter | 400 | `Unknown mock user: <value>`                           |
| Unknown ticket id                       | 404    | `Ticket not found: <id>`                                       |
| Illegal status transition               | 409    | `Illegal transition: TODO -> DONE_CANDIDATE`                   |
| Manager tries to approve a non-candidate| 409    | `Only DONE_CANDIDATE tickets can be marked DONE (current: ...)`|
