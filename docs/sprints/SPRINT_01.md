# Sprint 1: setup, REST API and DAL

- Team: Group G
- Classes: 14–24 September 2026
- Moodle deadline: not listed when checked on 17 September

## Goal

Upload and manage documents through the REST API, store them in PostgreSQL, and start the additional use case.

## Tasks

| Issue | Work | Status |
| --- | --- | --- |
| [#1](https://github.com/SimSta123/SWEN-3-Document-Management-System/issues/1) | Project setup, Docker, CI and templates | Merged in PR #8 |
| [#2](https://github.com/SimSta123/SWEN-3-Document-Management-System/issues/2) | API contract, OpenAPI and request examples | Ready |
| [#3](https://github.com/SimSta123/SWEN-3-Document-Management-System/issues/3) | Document entities, migrations and repositories | Todo |
| [#4](https://github.com/SimSta123/SWEN-3-Document-Management-System/issues/4) | Business components and layer mapping | Todo |
| [#5](https://github.com/SimSta123/SWEN-3-Document-Management-System/issues/5) | Upload and management endpoints | Todo |
| [#6](https://github.com/SimSta123/SWEN-3-Document-Management-System/issues/6) | Choose and start the additional use case | To decide |
| [#7](https://github.com/SimSta123/SWEN-3-Document-Management-System/issues/7) | Tests, demo and hand-in | Todo |

Track progress on the [board](https://github.com/users/YannPolini/projects/4). We'll agree owners and estimates together.

One idea for the additional use case is document status history: a document with multiple history entries. We haven't chosen it yet.

## Demo checklist

- [ ] Project builds and starts with Docker
- [ ] REST endpoints save and retrieve documents in PostgreSQL
- [ ] Additional entities and initial use-case implementation are included
- [ ] OpenAPI docs and request examples are up to date
- [ ] Tests cover the new behaviour and meet the course's >70% coverage target

The setup is merged and its checks passed. Document features are still open.

## Hand-in

The repository link has been emailed to the lecturer. Confirm the separate Moodle link submission and the sprint deadline. Submit the full project ZIP through Moodle and keep `main` runnable for the lecturer's build.

At the end of the sprint, record the review, remaining tasks and submission details using [the template](TEMPLATE.md).
