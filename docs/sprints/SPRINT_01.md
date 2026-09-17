# Sprint 1: setup, REST API, DAL

Status: setup prepared for review; domain implementation pending
Course class window: 2026-09-14 to 2026-09-24
Official Moodle deadline: not published in the 2026-09-17 snapshot
Team: Group G; assign owners together

## Goal

Build and run the REST backend and PostgreSQL in Docker, persist documents through a layered design, and start the additional use case with a documented API and meaningful tests.

## Work breakdown

| Work | Outcome | Status |
| --- | --- | --- |
| [#1 Bootstrap](https://github.com/SimSta123/SWEN-3-Document-Management-System/issues/1) | Java 25 / Spring Boot / Maven, Compose, health route, CI and team templates | Ready for review |
| [#2 API contract](https://github.com/SimSta123/SWEN-3-Document-Management-System/issues/2) | Document upload/manage routes, OpenAPI and request examples | Todo |
| [#3 Document DAL](https://github.com/SimSta123/SWEN-3-Document-Management-System/issues/3) | PostgreSQL migration, JPA entities, repositories and persistence tests | Todo |
| [#4 Business layer and mapping](https://github.com/SimSta123/SWEN-3-Document-Management-System/issues/4) | Separate BL/API/DAL models, component interfaces, MapStruct | Todo |
| [#5 Upload and management](https://github.com/SimSta123/SWEN-3-Document-Management-System/issues/5) | Working REST use cases with validation and error responses | Todo |
| [#6 Additional use case](https://github.com/SimSta123/SWEN-3-Document-Management-System/issues/6) | Team selects entities/workflow and starts implementation | Decision needed |
| [#7 Quality and hand-in](https://github.com/SimSta123/SWEN-3-Document-Management-System/issues/7) | >70% relevant test coverage, README/demo, ZIP and stable main | Todo |

These tasks are recorded in repository issues and assigned to the Sprint 1 milestone. Six sprint milestones exist. GitHub Projects board creation is pending authenticated Projects access. Assign owners/estimates as a team; none are invented here. Potential additional use case: document status history (Document 1:n HistoryLog), pending team agreement.

## Must-have demonstration

1. No build errors.
2. REST and PostgreSQL start.
3. REST endpoints persist data in the database.
4. Additional entities and use-case work are present.
5. API docs, request scripts and tests accompany the implementation.

The bootstrap alone does not complete Sprint 1. The current starter page only verifies service availability.

## Git and submission

Use feature branches and pull requests; keep main runnable. Repository link emailed to lecturer, per user report on 2026-09-17. Moodle's separate repository-link activity still needs confirmation. Submit the full project ZIP through Moodle; the teacher may build main at a random time after the deadline.

## Review / retrospective / evidence

Complete these fields when the sprint ends using TEMPLATE.md. Do not pre-mark the submission or tests complete.
