# ADR 0001: initial stack and boundaries

Date: 2026-09-17
Status: initial setup; frontend choice can be revisited before Sprint 2

## Context

The course requires Java 25+ / Spring Boot or C#/.NET, layered code, ORM repositories, a separate web UI, Docker Compose, and staged integration of queues/workers/storage/search/GenAI. The existing repository contained only a README and Git ignore file. The user chose a Spring Boot direction.

## Decision

Use Java 25, Spring Boot 4.1.1 and Maven Wrapper. Use PostgreSQL 17, Spring Data JPA, Flyway migrations, MapStruct and Bean Validation. H2 is test-only; PostgreSQL remains the actual database.

Start with HTML/CSS/JavaScript served by nginx, with same-origin `/api/` proxying to the backend. This minimises setup and matches the course exercise. Maven does not serve as a frontend framework. A React/TypeScript/Vite frontend can replace the static UI if agreed by the team.

Use three application layers: API facade with DTOs; business components with business models and interfaces; persistence entities/repositories. Add packages and real interfaces with the first domain feature instead of committing placeholder abstractions. MapStruct maps between layers. Flyway is enabled now; actual schema migrations arrive with the document/domain model.

Run the web server, API and database in Compose. Bind development HTTP ports to localhost, and keep the database internal. Readiness checks the actual database connection. On this laptop API port 8081 is used by another project, so the local untracked configuration uses 18081; repository defaults remain 8081.

Add RabbitMQ in Sprint 3; MinIO/OCR/indexing/Elasticsearch workers in Sprint 4; GenAI and mobile client in Sprint 5; integration/batch services in Sprint 6. Do not pre-start empty containers for services that have no behaviour yet.

## Consequences

Docker supports a Java 25 build even when the host has Java 21. IDE compilation still requires installing/configuring Java 25. Image tags track maintained releases within selected major lines; dependency updates should be deliberate and tested. CI runs backend tests and a real PostgreSQL-backed Compose smoke check. The scaffold does not demonstrate document persistence, upload or the additional use case.

## References

- Course project PDF and assessment matrix, checked 2026-09-17; full teaching materials are held in the team's course-document folder.
- https://docs.spring.io/spring-boot/system-requirements.html
- https://mapstruct.org/documentation/stable/reference/html/
- https://www.jacoco.org/jacoco/trunk/doc/changes.html
- https://docs.docker.com/compose/
