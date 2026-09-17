# Working together

## Branches and pull requests

1. Pick an issue from the current sprint and agree who takes it.
2. Create a branch from `main`: `feature/<issue>-description` or `fix/<issue>-description`.
3. Move the task to **In progress**.
4. Open a PR when it's ready, link the issue with `Closes #N`, and move it to **In review**.
5. Have a teammate review it and check that CI passes.
6. After merging and checking the result, move it to **Done**.

Keep `main` runnable. Branch protection isn't configured yet; that needs the repository owner.

## Board and sprints

[Kanban board](https://github.com/users/YannPolini/projects/4): **Backlog → Ready → In progress → In review → Done**.

Use the six repository milestones to group tasks by sprint. Add new issues to the board manually.

The board is under YannPolini and the repository under SimSta123. GitHub [requires the same owner](https://docs.github.com/en/issues/planning-and-tracking-with-projects/managing-your-project/adding-your-project-to-a-repository) to show it in the repository's Projects tab, so use the link above. Teammate editing access still needs to be added on the board.

| Sprint | Focus |
| --- | --- |
| 1 | Setup, REST API, DAL, mapping and additional use case |
| 2 | Web UI |
| 3 | RabbitMQ and mid-term review |
| 4 | MinIO, OCR and Elasticsearch |
| 5 | AI summaries and mobile app |
| 6 | Integration tests, XML batch processing and final review |

Copy [the sprint template](docs/sprints/TEMPLATE.md) when planning a sprint. Confirm deadlines in Moodle.

## Before merging

- Check the acceptance criteria, including error cases.
- Run the tests and update any affected docs.
- Keep secrets and generated files out of Git.
- Be able to explain your changes and sources.

Include source URLs in code comments and document tool assistance as required by the course. Submit the sprint ZIP separately through Moodle; pushing code doesn't submit it.
