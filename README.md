# SWEN3 Document Management System

Software Engineering 3 project, WS 2026. A document manager with OCR, full-text search and AI summaries.

The initial setup runs the frontend, backend and database. Document features are next.

[Kanban board](https://github.com/users/YannPolini/projects/4) · [Milestones](https://github.com/SimSta123/SWEN-3-Document-Management-System/milestones) · [Sprint 1](docs/sprints/SPRINT_01.md)

## Stack

- Backend: Java 25, Spring Boot 4.1.1, Maven Wrapper
- Database: PostgreSQL 17, Spring Data JPA, Flyway
- Mapping and validation: MapStruct, Bean Validation
- Frontend: HTML, CSS, JavaScript and nginx
- Tests: JUnit, Spring Boot Test, H2 and JaCoCo
- Local environment: Docker Compose

RabbitMQ, MinIO, OCR, Elasticsearch and the AI worker will follow in later sprints. See the [stack notes](docs/architecture/0001-stack.md).

## Run locally

Start Docker Desktop with Linux containers enabled.

1. Copy `.env.example` to `.env` and set a database password.
2. Run from the project root:

```sh
docker compose up --build --detach --wait
```

3. Open http://localhost/.

The first build downloads dependencies and runs the backend tests. Default ports are 80 for the frontend and 8081 for the API. Change `WEB_PORT` or `API_PORT` in `.env` if needed. PostgreSQL is only accessible inside Docker.

To check the services:

```sh
docker compose ps
curl http://localhost/api/health
```

The health check includes the database connection. More examples are in [requests/health.http](requests/health.http).

Stop with `docker compose down`; the database volume is kept. Keep `.env` and API keys out of Git. Changing the password in `.env` won't update an existing database volume.

## Backend development

Docker includes Java and Maven. For builds outside Docker, install JDK 25 and set `JAVA_HOME`, then run:

```sh
cd backend
./mvnw verify
```

On Windows, use `mvnw.cmd verify`. Coverage reports are in `backend/target/site/jacoco/index.html`. The current tests cover startup and health checks using H2; PostgreSQL repository tests will be added with the document model. The course coverage target is >70%.

To run the API outside Docker, set `DB_URL`, `DB_USER` and `DB_PASSWORD` for an accessible PostgreSQL instance, then run `./mvnw spring-boot:run`.

## Folders

- `backend/`: Spring Boot application
- `frontend/`: web page and nginx config
- `requests/`: API request examples
- `docs/`: stack notes, sprint plans and template
- `.github/`: CI, issue form and PR template

See [CONTRIBUTING.md](CONTRIBUTING.md) for the team workflow. Course materials stay in the separate course folder.

## References

- [Spring Initializr](https://start.spring.io/) — initial application and Maven Wrapper
- [Spring Boot testing](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html)
- [Docker Compose](https://docs.docker.com/compose/)
- [nginx proxy configuration](https://nginx.org/en/docs/http/ngx_http_proxy_module.html)
