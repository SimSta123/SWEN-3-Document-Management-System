# SWEN3 Document Management System

Software Engineering 3 Labor, WS 2026, Group G

Yann Polini and Simon Stangelberger

[Repository](https://github.com/SimSta123/SWEN-3-Document-Management-System) · [Kanban board](https://github.com/users/YannPolini/projects/4) · [Milestones](https://github.com/SimSta123/SWEN-3-Document-Management-System/milestones)

This is our document management system for SWEN3. We can upload PDFs, save document records and organise them into projects. A document belongs to one project at a time. Assigning it to another project moves it there.

On `sprint-4`, uploaded PDFs are stored in RustFS. A worker reads them with Tesseract and stores the extracted text in Elasticsearch. The backend and frontend search feature is still tracked in #46.

## Run the project

You need Docker Desktop with Linux containers enabled.

1. Copy `.env.example` to `.env`.
2. Set the database and RabbitMQ passwords, the RustFS access keys, `ELASTIC_PASSWORD` and `KIBANA_PASSWORD`.
3. Run this from the project folder:

   ```sh
   docker compose up --build --detach --wait
   ```

4. Open [localhost](http://localhost/) for the website.

The first build takes a while because it downloads the images and builds both Java applications. The `kibana-setup` container should finish and exit successfully. It only sets up Kibana's service account; the other services keep running.

| Service | Default address | Login |
| --- | --- | --- |
| Website | http://localhost/ | None |
| Backend API | http://localhost:8081 | None |
| RabbitMQ management | http://localhost:15672 | `paperless` / `RABBITMQ_PASSWORD` |
| RustFS console | http://localhost:9001 | RustFS access keys |
| Elasticsearch | http://localhost:9200 | `elastic` / `ELASTIC_PASSWORD` |
| Kibana | http://localhost:5601 | `elastic` / `ELASTIC_PASSWORD` |
| Logstash JSON input | http://localhost:18080 | None |

Passwords and keys come from your `.env`. `KIBANA_PASSWORD` is for the internal `kibana_system` account, not the Kibana website login. RustFS uses port 9000 for its S3 API.

You can change the website, backend and Elastic ports with `WEB_PORT`, `API_PORT`, `ELASTICSEARCH_PORT`, `KIBANA_PORT` and `LOGSTASH_PORT`. PostgreSQL is only reachable inside Docker. Published service ports are bound to localhost. Elasticsearch uses HTTP with password authentication for this local setup.

Run `docker compose down` to stop the project. This keeps the stored data in Docker volumes. Keep `.env` out of Git. Changing `POSTGRES_PASSWORD` in the file does not change the password in an existing database volume.

## How it works

The website uses HTML, CSS and JavaScript, served by nginx. The backend uses Java 25, Spring Boot 4.1.1 and Maven. PostgreSQL 17 stores document metadata and projects. RustFS stores the actual PDF files; it is our replacement for the MinIO named in the sprint slide.

```mermaid
flowchart LR
    Browser --> nginx --> Backend[REST backend]
    Backend --> PostgreSQL[(PostgreSQL)]
    Backend --> RustFS[(RustFS PDFs)]
    Backend --> Uploads[RabbitMQ upload queue]
    subgraph Worker[Separate worker application]
        OCR[Tesseract OCR]
        Indexer[Indexing listener]
    end
    Uploads --> OCR
    RustFS --> OCR
    OCR --> Results[RabbitMQ result queue] --> Indexer
    Indexer --> Elasticsearch[(Elasticsearch)]
    Logstash --> Elasticsearch
    Kibana --> Elasticsearch
```

For a PDF upload, the backend stores the file in RustFS and its metadata in PostgreSQL. It sends the document ID and storage key to RabbitMQ. The upload request can then finish while the worker processes the file separately.

The worker downloads the original PDF, converts every page to an image with PDFBox and runs Tesseract on those images. It sends the extracted text and document ID to a second RabbitMQ queue. The indexing listener reads that result and saves it in Elasticsearch. Both listeners run in the same worker application, which is separate from the REST backend.

In the backend, controllers handle HTTP requests, services handle the logic and repositories access the database. Spring injects these dependencies through constructors. MapStruct converts database entities into API response records. Flyway creates the tables, and Hibernate checks them against the entities. nginx forwards `/api/` requests to the backend and removes that prefix.

## Website and API

The dashboard groups documents by project, with an Unassigned group for documents without a project. We can save a title, upload a PDF and open a document's detail page to see its title, ID and project. Empty lists, failed requests and missing documents have their own messages.

The table below uses the backend paths. When calling through nginx, add `/api`, for example `/api/documents`.

| Method | Endpoint | Action |
| --- | --- | --- |
| POST | `/projects` | Create a project |
| GET | `/projects` | List projects |
| POST | `/documents` | Create a record from JSON or upload a multipart PDF |
| GET | `/documents` | List all documents |
| GET | `/documents/{documentId}` | Read a document |
| PUT | `/projects/{projectId}/documents/{documentId}` | Assign or move a document |
| GET | `/projects/{projectId}/documents` | List a project's documents |

See [project and document requests](requests/projects.http) and [health requests](requests/health.http) for examples.

### Upload a PDF

Use the website's PDF upload form, or send a multipart request with a `file` field. The API also accepts an optional `title`; otherwise it uses the filename.

```sh
curl --fail-with-body -F "file=@/path/to/notes.pdf;type=application/pdf" -F "title=Course notes" http://localhost/api/documents
docker compose logs --since 1m ocr-worker
```

On Windows, use `curl.exe` and replace the path with your PDF's path. The response should be 201 with the new document ID. After processing, the worker logs `OCR completed: documentId=<id>`.

Uploads are limited to 10 MiB. The REST endpoint rejects empty files and files without a `%PDF-` header. It only checks the header; the worker parses the PDF later. Creating a record from a JSON title still works, but that record has no PDF to process.

## OCR and indexing

The upload queue is `document.uploaded`. A stored PDF produces a message like `Document created: 42 with key: <objectKey>`. The worker uses the key to fetch the PDF from the same `documents` bucket as the backend.

[PdfOcr](ocr-worker/src/main/java/at/technikum/swen3/worker/PdfOcr.java) renders every page at 300 DPI and runs Tesseract with English language data. Tesseract is installed in the worker image, so it does not need to be installed on the host. Temporary PDFs and page images are removed after processing. Each page has a 60-second timeout.

The OCR result goes to `document.ocr.completed` as persistent JSON:

```json
{"documentId":42,"text":"Hello World"}
```

[IndexingListener](ocr-worker/src/main/java/at/technikum/swen3/worker/IndexingListener.java) checks the result and passes it to [ElasticsearchIndexer](ocr-worker/src/main/java/at/technikum/swen3/worker/ElasticsearchIndexer.java). The `documents` index stores `documentId` as `long` and the extracted `text` as searchable text. Its Elasticsearch ID matches the document ID in PostgreSQL. Processing the same result again replaces the entry instead of creating a duplicate.

The indexer creates the index if needed and writes with `refresh=wait_for`. It acknowledges the result message after Elasticsearch accepts the entry and makes it visible to search. The search API can use the stored `documentId` to look up the document's title and metadata.

`OCR_QUEUE`, `OCR_RESULT_QUEUE` and `ELASTICSEARCH_INDEX` change the default queue and index names. The worker also supports `TESSERACT_COMMAND`, `OCR_LANGUAGE` and `OCR_PAGE_TIMEOUT` in its environment. Other OCR languages need their trained data installed in the image.

### Failed messages

Both RabbitMQ queues are durable. Failed upload processing goes to `document.uploaded.failed`; invalid OCR results and indexing failures go to `document.ocr.completed.failed`. These queues have no consumer, so we can inspect or manually retry the messages instead of retrying a bad message forever. Changing a queue name also changes its failed queue to that name plus `.failed`.

Blank or invalid UTF-8 upload messages are rejected. Older metadata-only messages and the text/JSON test messages are still logged, but do not start OCR because they have no storage key. Spring acknowledges messages after successful processing and reconnects if RabbitMQ goes offline.

An older local queue may exist without the failed-message settings. RabbitMQ will reject the new arguments for that queue. Drain its pending messages before recreating it, or use a new queue name for testing. Do not delete queued uploads or OCR results.

Use `docker compose logs -f ocr-worker` for worker logs and the RabbitMQ management page to inspect queues.

## RustFS and ELK

Compose starts RustFS, the worker, Elasticsearch, Logstash and Kibana alongside the database, RabbitMQ, backend and frontend. The backend and worker wait for RustFS readiness. The worker and Logstash wait for Elasticsearch, and Kibana waits for its account setup to finish.

Elasticsearch, Logstash and Kibana all use version 9.5.6. Their stored data uses Docker volumes. Elasticsearch runs as a single node.

The [Logstash pipeline](docker/elk/logstash.conf) accepts JSON over HTTP, keeps a persistent queue and writes events to `paperless-logs-*` in Elasticsearch. Kibana connects to Elasticsearch so we can inspect the data. This is a simple ELK connection setup; it does not collect every application's logs automatically. OCR text is stored separately in `documents`.

## Validation and API errors

The website checks document titles. The API and service validate titles, project names and IDs. The service checks also apply when it is called directly. Blank values, values over the length limits and invalid IDs are rejected.

[ApiExceptionHandler](backend/src/main/java/at/technikum/swen3/paperless/controller/ApiExceptionHandler.java) returns JSON problem responses: 400 for invalid input, 404 for missing records, 409 for database conflicts and 503 for temporary database failures. Other failures return 500. Full errors stay in the logs; responses do not include SQL or connection details.

The request filter logs HTTP requests. Services log their operations and rejected inputs. Service DEBUG logging is configured in [application.properties](backend/src/main/resources/application.properties).

## Tests and checks

Backend unit tests use JUnit and Mockito for service logic. HTTP integration tests run against H2 with RabbitMQ mocked. We check PostgreSQL, RabbitMQ, RustFS and Elasticsearch separately in the running Compose stack.

The Docker builds run the Java tests. With JDK 25 installed, you can also run `./mvnw verify` from `backend/` or `ocr-worker/`. On Windows, use `.\mvnw.cmd verify`. Backend coverage is written to `backend/target/site/jacoco/index.html`.

On 13 November 2026, 66 backend tests and 44 worker tests passed. Worker tests include actual Tesseract recognition of a two-page image-only PDF, message validation and Elasticsearch HTTP behaviour. [GitHub CI](.github/workflows/ci.yml) also passed the full Compose startup and the checks below on `sprint-4`.

| Script | What it checks |
| --- | --- |
| `python scripts/check-ocr-worker.py` | Message logs, acknowledgements and failed-message routing |
| `python scripts/check-ocr-flow.py` | PDF upload, RustFS retrieval, OCR, Elasticsearch search, repeated delivery and invalid results |
| `python scripts/check-elk.py` | Kibana availability and a Logstash event arriving in Elasticsearch |

Use a disposable test stack. These scripts create test data and read failed messages. Set `RABBITMQ_PASSWORD` and `ELASTIC_PASSWORD` in your shell to match that stack. The OCR flow check uses `API_URL`, defaulting to `http://localhost:8081`; CI sets it to the nginx API address.

The checks support `RABBITMQ_MANAGEMENT_URL`, `ELASTICSEARCH_URL`, `ELASTICSEARCH_INDEX`, `KIBANA_URL` and `LOGSTASH_URL` for other addresses. They also accept `ELASTICSEARCH_PASSWORD` in place of `ELASTIC_PASSWORD`. The worker checks use the configured queue names. The Elasticsearch checks wait through temporary 404/503 responses while the first index shards start, then fail if the data does not appear in time.

The automated Hello search calls Elasticsearch directly. The required upload and search through the actual website still need the complete search feature and the final Sprint 4 check in #47.

## Code and references

- [Backend](backend/src/main/java/at/technikum/swen3/paperless): controllers, services, entities and repositories
- [Database migrations](backend/src/main/resources/db/migration)
- [Frontend](frontend/public)
- [Worker application](ocr-worker/src/main/java/at/technikum/swen3/worker)
- [Docker Compose setup](compose.yaml)
- [Spring AMQP listeners](https://docs.spring.io/spring-amqp/reference/amqp/receiving-messages/async-annotation-driven.html) and [error handling](https://docs.spring.io/spring-amqp/reference/amqp/exception-handling.html)
- [Elasticsearch index API](https://www.elastic.co/docs/api/doc/elasticsearch/operation/operation-index) and [Docker setup](https://www.elastic.co/docs/deploy-manage/deploy/self-managed/install-elasticsearch-docker-basic)
- [Kibana Docker setup](https://www.elastic.co/docs/deploy-manage/deploy/self-managed/install-kibana-with-docker) and [Logstash configuration](https://www.elastic.co/docs/reference/logstash/docker-config)

One problem we ran into was nginx forwarding the wrong path. Removing `/api/` fixed it. We also added validation in the service because frontend and controller checks do not cover direct service calls. For Sprint 4, we had to account for Elasticsearch taking a moment to make a new index searchable, so the checks wait for it within a time limit.

We work on sprint branches and check the whole app before merging into `main`. Tasks are on the board linked above. We used Codex to help with implementation, tests and documentation, and both go through the code before the review.
