# Working together

## Sprint branches

1. Pick an issue from the current sprint and agree who takes it.
2. At the start of a sprint, create a shared branch from `main`, such as `sprint-2` or `sprint-3`.
3. Create feature or fix branches from that sprint branch: `feature/<issue>-description` or `fix/<issue>-description`.
4. Move the task to **In progress** while implementing it.
5. Test the feature, commit it with the issue numbers, merge it into the sprint branch and push. Move the task to **In review** for the final integration check. Pull requests and teammate reviews are optional.
6. Pull the latest sprint branch before merging or pushing. Resolve conflicts without overwriting the other person's changes.
7. Once the whole sprint builds, runs and passes its checks, merge the sprint branch into `main`, push and move completed tasks to **Done**. Start the next sprint branch from the updated `main`.

Keep `main` runnable. A separate `develop` branch is unnecessary with this workflow. Branch protection isn't configured yet; that needs the repository owner.

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

Use the board and milestones to plan each sprint. Confirm deadlines in Moodle.

## Before merging

- Check the acceptance criteria, including error cases.
- Run the tests and update any affected docs.
- Keep secrets and generated files out of Git.
- Be able to explain your changes and sources.

Include source URLs in code comments and document tool assistance as required by the course. Submit the sprint ZIP separately through Moodle; pushing code doesn't submit it.
