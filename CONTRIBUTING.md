# Working together

## Sprint branches

1. Assign yourself a task from the current sprint.
2. Start each sprint with a branch from `main`, for example `sprint-2`.
3. Make a feature branch from the sprint branch, for example `feature/13-dashboard`.
4. Move the task to **In progress** and do the work.
5. Test it, commit it with the issue number and merge it into the sprint branch. Move the task to **In review**.
6. Pull before merging or pushing so you have the other person's changes too.
7. Check the whole app before submission. When the sprint works, merge it into `main` and move the finished tasks to **Done**. Create the next sprint branch from the updated `main`.

We merge branches directly, so we do not need pull requests or teammate reviews. **In review** means the work is implemented and waiting for the final app check. Keep `main` working and use the sprint branch for work in progress.

## Board and sprints

[Kanban board](https://github.com/users/YannPolini/projects/4): **Backlog → Ready → In progress → In review → Done**.

Use the milestones to group tasks by sprint. Add new issues to the board.

The board belongs to YannPolini and the repository to SimSta123, so open the board using the link above or at the top of the README.

| Sprint | Focus |
| --- | --- |
| 1 | Setup, REST API, DAL, mapping and additional use case |
| 2 | Web UI |
| 3 | RabbitMQ and mid-term review |
| 4 | MinIO, OCR and Elasticsearch |
| 5 | AI summaries and mobile app |
| 6 | Integration tests, XML batch processing and final review |

Use the board and milestones to plan each sprint. Confirm deadlines in Moodle.

## Before submission

- Check that each task works, including invalid input and errors.
- Run the tests and update any affected docs.
- Keep passwords, `.env` and build output out of Git.
- Be able to explain your changes and sources.

Include source URLs in code comments and document tool assistance as required by the course. Submit the sprint ZIP separately through Moodle; pushing code doesn't submit it.
