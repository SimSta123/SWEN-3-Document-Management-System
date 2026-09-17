# 0001: Initial stack

Date: 2026-09-17

## Choices

We use Java 25, Spring Boot 4.1.1 and Maven, with PostgreSQL 17 for storage. This follows the course's Java option.

Spring Data JPA handles persistence, Flyway manages schema changes, MapStruct maps between layers, and Bean Validation checks input. H2 is used for tests only. The document model and migrations are still to be added.

The frontend starts with HTML, CSS and JavaScript served by nginx. It matches the course exercise and keeps the setup small. We can revisit the frontend choice before Sprint 2.

## Structure

The application will have three layers:

- API: controllers and request/response DTOs
- Business: models, rules and component interfaces
- DAL: database entities and repositories

We'll add these with the first document feature, using MapStruct between the models.

Docker Compose runs nginx, the API and PostgreSQL. nginx forwards `/api/` requests to the backend. HTTP ports are bound to localhost; PostgreSQL stays inside the Docker network. The readiness endpoint checks the database connection.

## Later sprints

- Sprint 3: RabbitMQ
- Sprint 4: MinIO, OCR workers and Elasticsearch
- Sprint 5: AI summary worker and mobile app
- Sprint 6: integration tests and XML batch processing

## Build and tests

Docker builds with Java 25. Local IDE builds also need JDK 25. CI runs the backend tests and starts the full Compose stack for a health check. Image tags follow the selected major versions; updates need testing.

## References

- Course project PDF and grading rubric, checked 2026-09-17
- [Spring Boot requirements](https://docs.spring.io/spring-boot/system-requirements.html)
- [MapStruct](https://mapstruct.org/documentation/stable/reference/html/)
- [JaCoCo Java support](https://www.jacoco.org/jacoco/trunk/doc/changes.html)
- [Docker Compose](https://docs.docker.com/compose/)
