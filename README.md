# SWEN3 Document Management System

Software Engineering 3 Labor, WS 2026 — Group G
Yann Polini and Simon Stangelberger

We are building a document management system as our semester project. The aim is to upload and organise documents, with text recognition, search and AI summaries added during the later sprints.

## Sprint 1

Our first step was to set up the backend, database and a basic web page. Docker Compose runs the services together, and GitHub Actions runs the build checks.

We added a small database example to follow a request through the controller, service and repository. `GET /one` reads a message from PostgreSQL, while `GET /one/i` returns a fixed response.

Our additional use case is organising documents into projects. We can create projects with a name and description, create document records with a title, and assign them to a project. A project can contain several documents. Each document can belong to one project at a time, and assigning it to another project moves it there.

The records and their relationships are stored in PostgreSQL. Flyway creates the tables, JPA repositories handle database access, and MapStruct converts database entities into the records returned by the API. For now, this feature stores document titles and project membership. File uploads and the project interface are still to come.

## Run the project

You need Docker Desktop with Linux containers enabled.

1. Copy `.env.example` to `.env` and set `POSTGRES_PASSWORD`.
2. From the project folder, run:

   ```sh
   docker compose up --build --detach --wait
   ```

3. Open [localhost](http://localhost/) for the basic web page. The API runs at [localhost:8081](http://localhost:8081).

The default ports are 80 for the web page, 8081 for the API and 5432 for PostgreSQL. If needed, change `WEB_PORT` and `API_PORT` in `.env`; the database port is set in `compose.yaml`.

Stop the services with `docker compose down`. The database volume is kept. Keep `.env` out of Git. Changing its password does not change the password in an existing database volume.

## Try the API

The [project requests](requests/projects.http) show the workflow: create a project, create a document record, assign it, then list the project's documents. Use the IDs returned by the create requests and adjust the API port if you changed it.

| Method | Endpoint | Action |
| --- | --- | --- |
| POST | `/projects` | Create a project |
| GET | `/projects` | List projects |
| POST | `/documents` | Create a document record |
| PUT | `/projects/{projectId}/documents/{documentId}` | Assign or move a document |
| GET | `/projects/{projectId}/documents` | List a project's documents |

There are also [health requests](requests/health.http) to check the backend and database connection.

## Tests

The Docker build runs the automated tests. To run them locally with JDK 25, use `./mvnw verify` from `backend/`, or `mvnw.cmd verify` on Windows. The coverage report is generated at `backend/target/site/jacoco/index.html`.

The two existing tests check the database connection and backend readiness using H2. We also checked project creation, document assignment, moving documents and error responses against PostgreSQL. Saved data remained after restarting the backend and database. Automated tests for the project feature are the next step.

## Technologies and team workflow

We use Java 25, Spring Boot 4.1.1, Maven, PostgreSQL 17, Spring Data JPA, Flyway and MapStruct. The basic frontend uses HTML, CSS and JavaScript served by nginx.

We track tasks on our [Kanban board](https://github.com/users/YannPolini/projects/4) and group them by [sprint milestones](https://github.com/SimSta123/SWEN-3-Document-Management-System/milestones). See [CONTRIBUTING.md](CONTRIBUTING.md) for the team workflow.
