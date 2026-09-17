# Team workflow

Keep `main` runnable: the lecturer may build it after the submission deadline. Do not treat a Git push as a Moodle submission.

## Daily work

1. Pick a small issue from the current sprint and assign it as a team.
2. Create `feature/<issue>-short-description` or `fix/<issue>-short-description` from the agreed integration branch.
3. Move the issue to In progress. Keep one or two active tasks per person.
4. Commit focused changes, with tests and relevant documentation.
5. Open a PR, link the issue using `Closes #N`, and move it to In review.
6. A teammate reviews it; CI and acceptance criteria must pass before merge.
7. Mark Done only once the work is merged and verified.

Use `develop` as an integration branch if the team adopts full GitFlow; merge a reviewed, tested sprint increment into `main`. For this initial bootstrap the PR targets `main`. Do not create release branches or merge into main just to make the board look complete. Branch protection requires the repository owner's permissions; it is not configured by these files.

## Board and sprints

Suggested columns: **Todo → In progress → In review → Done**. Keep future work in a Backlog column or a separate filtered view. Use six sprint milestones and a board Sprint/iteration field when available. The course windows are planning context, not verified Moodle deadlines.

- Sprint 1: setup, REST API, DAL, mapping and additional-use-case start.
- Sprint 2: web UI and integration.
- Sprint 3: queuing and mid-term review.
- Sprint 4: workers, MinIO, OCR and Elasticsearch.
- Sprint 5: GenAI and smartphone app.
- Sprint 6: integration tests, scheduled XML batch processing and finalisation.

Use [the sprint template](docs/sprints/TEMPLATE.md) to record the goal, scope, owners, acceptance criteria, review and retrospective. Keep issues, PRs and the board current. Do not invent deadlines or silently assign work to teammates.

## Definition of done

- Acceptance criteria work, including important invalid-input/error cases.
- Automated checks pass; meaningful tests cover changed behaviour.
- Source quality, mapping/layer boundaries, validation and logging are reviewed.
- API contract and design/setup documentation reflect the change.
- No secrets, generated output or personal course files are committed.
- The author can explain what the code does, why it was written, and relevant external sources.
- Review, merge, sprint submission and review appointment are recorded separately.

The course asks for exact URLs in comments where external sources are used. Record tool assistance honestly according to the lecturer's requirements and make sure every team member understands their submitted work.
