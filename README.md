# Paperless — SWEN3 Document Management System

Team project for Software Engineering 3, WS 2026. The target application uploads and manages documents, runs asynchronous OCR, supports full-text search, and generates summaries.

**Current state:** development scaffold. Docker starts the web frontend, Spring Boot API and PostgreSQL. The landing page checks service availability. Document upload, persistence entities, search and workers are still backlog items; Sprint 1 is not complete.

## Stack

| Part | Choice | Stage |
| --- | --- | --- |
| Backend | Java 25, Spring Boot 4.1.1, Maven Wrapper 3.9.16 | Configured |
| Data access | Spring Data JPA/Hibernate, PostgreSQL 17, Flyway | Dependencies/infrastructure configured; domain model pending |
| Mapping / validation | MapStruct 1.6.3, Jakarta Bean Validation | Configured; domain mappings pending |
| Frontend | HTML, CSS and JavaScript, served by nginx | Starter page only; full UI in Sprint 2 |
| Tests | JUnit, Spring Boot Test, H2 for startup tests, JaCoCo | Startup checks plus Compose smoke check |
| Deployment | Docker Compose | Web/API/database |
| Later services | RabbitMQ; MinIO; Tesseract/Ghostscript OCR; Elasticsearch; GenAI worker; scheduled XML batch service | Sprints 3-6 |

Maven builds the Java backend. It is not a frontend framework. The simple frontend follows the course's HTML/CSS/JavaScript exercise; a framework can be chosen before Sprint 2 if the team prefers.

## Run with Docker

Install Docker Desktop and start its Linux-container engine. No local Java or Maven installation is needed for this path.

1. Copy `.env.example` to `.env` and set a local development database password.
2. From the repository root run:

```sh
docker compose up --build --detach --wait
```

3. Open `http://localhost/`. The page should report that the service is available.

The first build downloads Java/Maven dependencies and runs backend tests. Default ports are web 80 and API 8081. If occupied, change `WEB_PORT` / `API_PORT` in `.env`; container ports stay unchanged. PostgreSQL stays on the private Compose network and uses a named volume.

Checks:

```sh
docker compose ps
curl http://localhost/api/health
curl http://localhost:8081/actuator/health/readiness
```

Readiness includes a database connection check. The web route proxies to backend readiness, demonstrating web → API → database connectivity. Request examples are in [requests/health.http](requests/health.http).

Stop this project's services with `docker compose down`. The database volume is retained. Never commit `.env` or API keys. Replacing `.env`'s password does not automatically change credentials in an existing PostgreSQL volume.

## Develop the backend

For local IDE builds, install **JDK 25** and set `JAVA_HOME` accordingly. Java 21 does not satisfy this project's target.

```sh
cd backend
./mvnw verify
```

On Windows use `mvnw.cmd verify`. The wrapper downloads Maven; a separate Maven installation is unnecessary. Tests activate an H2 test profile. H2 startup checks do not replace PostgreSQL repository tests; add those with the domain model.

To run the backend outside Docker, configure `DB_URL`, `DB_USER` and `DB_PASSWORD` for a reachable PostgreSQL instance, then run `./mvnw spring-boot:run`. The default Compose database does not publish a host port.

JaCoCo reports are generated at `backend/target/site/jacoco/index.html`. There is no artificial coverage claim or gate for the empty domain model. Sprint 1 must add meaningful domain/repository tests and meet the course's >70% relevant coverage requirement.

## Project structure and next work

- `backend/`: REST facade, business and persistence implementation as features are added.
- `frontend/`: nginx configuration and static UI.
- `compose.yaml`: development stack.
- `requests/`: repeatable API calls.
- `docs/architecture/`: stack and architecture decisions.
- `docs/sprints/`: Sprint 1 plan and reusable sprint template.
- `.github/`: automated checks, issue form and PR checklist.

Start with [Sprint 1](docs/sprints/SPRINT_01.md), [team workflow](CONTRIBUTING.md), and [stack decision](docs/architecture/0001-stack.md). Detailed course PDFs and personal course state stay in the separate NAS class folder, not this public repository.

## Sources

- [Spring Boot requirements](https://docs.spring.io/spring-boot/system-requirements.html)
- [Spring Initializr](https://start.spring.io/) generated the initial Java application and Maven Wrapper.
- [Spring Boot testing](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html)
- [MapStruct reference](https://mapstruct.org/documentation/stable/reference/html/)
- [Docker Compose](https://docs.docker.com/compose/)
- [nginx proxy module](https://nginx.org/en/docs/http/ngx_http_proxy_module.html)
- [GitHub Projects guide](https://docs.github.com/en/issues/planning-and-tracking-with-projects/learning-about-projects/quickstart-for-projects)
