# Task Tracker

A small Jira-inspired task tracker built as a Spring Boot 3 + React + PostgreSQL
stack. Tickets flow through `TODO → IN_PROGRESS → DONE_CANDIDATE` for the
assignee, and a manager performs the final `DONE_CANDIDATE → DONE` approval.
TODO: implement sdd from marketplace

## Quickstart

```bash
cp .env.example .env
docker compose up --build
```

- Frontend: <http://localhost:5173>
- Backend:  <http://localhost:8080>
- Swagger:  <http://localhost:8080/swagger-ui.html>

## Documentation

- [API reference](docs/api.md)
- [Local setup](docs/local-setup.md)
- [Testing](docs/testing.md)
- [Project backlog](docs/project-tasks.md)

## Make targets

```bash
make up       # docker compose up --build -d
make down     # docker compose down
make backend  # mvn -f backend/pom.xml spring-boot:run
make frontend # npm --prefix frontend run dev
make test     # full test cycle via scripts/run-all-tests.sh
```
