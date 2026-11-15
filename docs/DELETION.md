# Document deletion

`DELETE /documents/{id}` removes the original PDF from RustFS, the OCR text from Elasticsearch and the metadata record from PostgreSQL. A successful response is 204. Invalid IDs return 400; missing documents return 404. An unavailable search service returns 503, with details kept in the logs.

## Protection against late OCR results

The database record is the source of truth. The worker checks it before downloading a PDF and again after recognition. If the document was deleted, the job ends without publishing text. Temporary files are still removed. A download/OCR failure caused by a completed deletion is also discarded.

Checking existence alone cannot protect indexing: deletion could happen between that check and the Elasticsearch write. The backend therefore locks the document row before deletion. `DocumentLifecycle.whilePresent` takes the same PostgreSQL row lock and holds it until its Elasticsearch write finishes.

- Indexing starts first: deletion waits, then removes the finished index entry and the record.
- Deletion starts first: indexing waits, then finds no record and skips the stale result.
- Deletion rolls back: the waiting index operation can continue against the surviving record.

This works across separate backend/worker processes and multiple worker instances. It does not depend on an in-memory flag, a temporary Elasticsearch deletion marker or clearing a whole queue. Deleted records are actually removed; no soft-delete column or new migration is needed.

The worker now needs the database URL, user and password, supplied by Compose. It waits for backend readiness so Flyway has created the tables. It uses JDBC only to check and lock document records; OCR and Elasticsearch writes still run in the separate worker application.

Upload publishing uses a transacted RabbitMQ channel synchronized with the backend database transaction. The message becomes available after the record commits, so a newly created document cannot be mistaken for a deleted one.

## Failure behaviour

Stale jobs are acknowledged. Infrastructure failures still follow the existing failed-queue path; an unavailable database never allows unchecked indexing. There is no automatic failed-message recovery.

The row lock orders deletion and indexing; it does not make PostgreSQL, RustFS and Elasticsearch one distributed transaction. A failed delete can already have removed an external object. The database record remains if Elasticsearch deletion fails, and repeating DELETE can finish the cleanup. A failed request is not reported as a successful deletion.

## Checks

The normal Docker builds run backend HTTP tests and worker tests. The worker suite includes both lock orderings, deletion rollback, missing records, lock release after errors, stale result acknowledgement and OCR cleanup.

Run `python scripts/check-deletion-flow.py` on a **disposable Compose stack**. Set `RABBITMQ_PASSWORD`, `ELASTIC_PASSWORD`, `RUSTFS_ACCESS_KEY` and `RUSTFS_SECRET_KEY` to that stack's values. It creates its own documents and checks:

- An indexed document disappears from PostgreSQL, RustFS and Elasticsearch, and from backend search results.
- An upload and a result queued before deletion are skipped after the worker restarts.
- Redelivering both messages after deletion cannot recreate the entry.
- Actual Tesseract recognition is held open by the test fixture, the document is deleted, and recognition then finishes without publishing its text.

The script temporarily stops/restarts the selected Compose project's worker and starts one controlled test worker. It restores the normal worker in cleanup. Use the same Compose project/files/environment as the target stack. Optional addresses are `API_URL`, `RABBITMQ_MANAGEMENT_URL`, `ELASTICSEARCH_URL` and `RUSTFS_URL`; queue, index and bucket overrides are also supported.

CI additionally runs `DocumentLifecycleTest` against a dedicated `lifecycle_test` PostgreSQL database, separate from the application database. The `lifecycle.test.url/user/password` Maven properties are for this disposable test database only: the test creates and drops its own `documents` table there.

References: [PostgreSQL row locks](https://www.postgresql.org/docs/17/explicit-locking.html), [Spring Data JPA locking](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html), [Spring AMQP transactions](https://docs.spring.io/spring-amqp/reference/amqp/transactions.html).
