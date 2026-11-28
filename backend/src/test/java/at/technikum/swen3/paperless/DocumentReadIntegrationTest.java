package at.technikum.swen3.paperless;

import at.technikum.swen3.paperless.rabbitMQ.SummaryResultListener;
import at.technikum.swen3.paperless.repository.DocumentRepository;
import at.technikum.swen3.paperless.repository.ProjectRepository;
import at.technikum.swen3.paperless.service.DocumentService;
import at.technikum.swen3.paperless.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.mockito.ArgumentCaptor;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

// Check controller, service, mapper and persistence together using real HTTP.
// https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DocumentReadIntegrationTest {
    @MockitoBean
    private software.amazon.awssdk.services.s3.S3Client s3;

    @LocalServerPort private int port;
    @Autowired private ProjectService service;
    @Autowired private DocumentService docService;
    @Autowired private DocumentRepository documents;
    @Autowired private ProjectRepository projects;
    @Autowired private SummaryResultListener summaryResults;

    @BeforeEach
    void clearRecords() {
        documents.deleteAll();
        projects.deleteAll();
    }

    @MockitoBean
    private RabbitTemplate rabbit;

    @MockitoBean
    private at.technikum.swen3.paperless.service.ElasticsearchClient elasticsearch;

    @Test
    void deletingPdfRemovesMetadataOriginalAndSearchEntry() throws Exception {
        upload(PDF_HEADER, "Delete me");
        var document = documents.findAll().getFirst();
        assertThat(request("DELETE", "/documents/" + document.getId(), null).statusCode()).isEqualTo(204);
        assertThat(documents.existsById(document.getId())).isFalse();
        verify(s3).deleteObject(any(software.amazon.awssdk.services.s3.model.DeleteObjectRequest.class));
        verify(elasticsearch).delete(document.getId());
        assertThat(request("GET", "/documents/" + document.getId(), null).statusCode()).isEqualTo(404);
    }

    @Test
    void failedExternalDeletionKeepsMetadataAndCanBeRetried() throws Exception {
        var document = docService.createDocument("Retry deletion");
        doThrow(new java.io.IOException("Elasticsearch unavailable")).when(elasticsearch).delete(document.id());
        assertThat(request("DELETE", "/documents/" + document.id(), null).statusCode()).isEqualTo(503);
        assertThat(documents.existsById(document.id())).isTrue();
        doNothing().when(elasticsearch).delete(document.id());
        assertThat(request("DELETE", "/documents/" + document.id(), null).statusCode()).isEqualTo(204);
        assertThat(documents.existsById(document.id())).isFalse();
    }

    @Test
    void deletionValidatesIdAndPreservesNotFoundResponse() throws Exception {
        assertThat(request("DELETE", "/documents/0", null).statusCode()).isEqualTo(400);
        assertThat(request("DELETE", "/documents/9223372036854775807", null).statusCode()).isEqualTo(404);
    }

    private HttpResponse<String> request(String method, String path, String body) throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                            : HttpRequest.BodyPublishers.ofString(body))
                    .build();
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }

    @Test
    void listIncludesAssignedAndUnassignedDocuments() throws Exception {
        var unassigned = docService.createDocument("Unassigned notes");
        var assigned = docService.createDocument("Project notes");
        var project = service.createProject("University", "");
        service.assignDocument(project.id(), assigned.id());
        var response = request("GET", "/documents", null);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains(
                "{\"id\":" + unassigned.id() + ",\"title\":\"Unassigned notes\",\"projectId\":null,\"summary\":null}",
                "{\"id\":" + assigned.id() + ",\"title\":\"Project notes\",\"projectId\":" + project.id()
                        + ",\"summary\":null}");
    }

    @Test
    void emptyLibraryReturnsEmptyArray() throws Exception {
        var response = request("GET", "/documents", null);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("[]");
    }

    @Test
    void detailReturnsSelectedRecordWithProjectAndSummary() throws Exception {
        var document = docService.createDocument("Selected document");
        var project = service.createProject("Course", "");
        service.assignDocument(project.id(), document.id());
        String summary = "Notes from the course. ".repeat(12) + "Final remarks.";
        docService.saveSummary(document.id(), summary);
        var response = request("GET", "/documents/" + document.id(), null);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("{\"id\":" + document.id()
                + ",\"title\":\"Selected document\",\"projectId\":" + project.id()
                + ",\"summary\":\"" + summary + "\"}");
    }

    @Test
    void missingDocumentReturnsNotFound() throws Exception {
        assertThat(request("GET", "/documents/9223372036854775807", null).statusCode()).isEqualTo(404);
    }

    @Test
    void existingTitleCreationStillWorksAndRejectsBlankTitles() throws Exception {
        var created = request("POST", "/documents", "{\"title\":\"  Sprint 2 notes  \"}");
        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(created.body()).contains("\"title\":\"Sprint 2 notes\"", "\"projectId\":null");
        assertThat(request("GET", "/documents", null).body()).contains("\"title\":\"Sprint 2 notes\"");
        assertThat(request("POST", "/documents", "{\"title\":\" \"}").statusCode()).isEqualTo(400);
    }

    private HttpResponse<String> upload(byte[] file, String title) throws Exception {
        String boundary = "swen3-upload-test-boundary";
        var body = new ByteArrayOutputStream();
        if (file != null) {
            body.writeBytes(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"notes.pdf\""
                    + "\r\nContent-Type: application/pdf\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            body.writeBytes(file);
            body.writeBytes("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        if (title != null) {
            body.writeBytes(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"title\"\r\n\r\n"
                    + title + "\r\n").getBytes(StandardCharsets.UTF_8));
        }
        body.writeBytes(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        try (HttpClient client = HttpClient.newHttpClient()) {
            var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/documents"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build();
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }

    @Test
    void failedFileDoesNotPreventTheNextSummaryFromBeingSavedAndUsed() throws Exception {
        var project = service.createProject("Two PDFs", "");
        long failedId = addPdfToProject(project.id(), "First PDF");
        long nextId = addPdfToProject(project.id(), "Second PDF");
        String path = "/projects/" + project.id() + "/summary";

        assertThat(request("POST", path, null).body()).contains("\"status\":\"WAITING\"");
        receiveSummaryResult("{\"documentId\":" + failedId + ",\"failed\":true}");
        assertThat(documents.findById(failedId).orElseThrow().isSummaryFailed()).isTrue();
        assertThat(request("GET", path, null).body()).contains("\"status\":\"WAITING\"");

        receiveSummaryResult("{\"documentId\":" + nextId + ",\"summary\":\"Second PDF summary\"}");

        assertThat(documents.findById(nextId).orElseThrow().getSummary()).isEqualTo("Second PDF summary");
        assertThat(request("GET", path, null).body())
                .contains("\"status\":\"QUEUED\"", "\"readyDocuments\":1", "\"totalDocuments\":2");
        var published = ArgumentCaptor.forClass(Message.class);
        verify(rabbit).send(eq(""), eq("project.summary.requested"), published.capture());
        assertThat(new String(published.getValue().getBody(), StandardCharsets.UTF_8))
                .contains("Second PDF summary").doesNotContain("First PDF");

        var requestId = projects.findById(project.id()).orElseThrow().getSummaryRequestId();
        String projectResult = "{\"projectId\":" + project.id() + ",\"requestId\":\"" + requestId
                + "\",\"summary\":\"Project description\"}";
        summaryResults.receiveProjectSummary(new Message(projectResult.getBytes(StandardCharsets.UTF_8)));
        assertThat(request("GET", path, null).body())
                .contains("\"status\":\"COMPLETED\"", "\"summary\":\"Project description\"");
    }

    @Test
    void projectWithOnlyFailedFilesCanRetryAndLateFailuresKeepSavedSummaries() throws Exception {
        var project = service.createProject("One PDF", "");
        long documentId = addPdfToProject(project.id(), "Retry PDF");
        String path = "/projects/" + project.id() + "/summary";

        assertThat(request("POST", path, null).body()).contains("\"status\":\"WAITING\"");
        String failure = "{\"documentId\":" + documentId + ",\"failed\":true}";
        receiveSummaryResult(failure);
        assertThat(request("GET", path, null).body()).contains("\"status\":\"FAILED\"");

        receiveSummaryResult("{\"documentId\":" + documentId + ",\"summary\":\"Recovered summary\"}");
        receiveSummaryResult(failure);

        var saved = documents.findById(documentId).orElseThrow();
        assertThat(saved.getSummary()).isEqualTo("Recovered summary");
        assertThat(saved.isSummaryFailed()).isFalse();
        assertThat(request("POST", path, null).body()).contains("\"status\":\"QUEUED\"");
    }

    private long addPdfToProject(long projectId, String title) throws Exception {
        assertThat(upload(PDF_HEADER, title).statusCode()).isEqualTo(201);
        long id = documents.findAll().stream().filter(file -> file.getTitle().equals(title))
                .findFirst().orElseThrow().getId();
        service.assignDocument(projectId, id);
        return id;
    }

    private void receiveSummaryResult(String json) {
        summaryResults.receive(new Message(json.getBytes(StandardCharsets.UTF_8)));
    }

    private static final byte[] PDF_HEADER = "%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII);

    @Test
    void pdfUploadCreatesRecordUsingFilenameAndPublishesMessage() throws Exception {
        var response = upload(PDF_HEADER, null);
        assertThat(response.statusCode()).isEqualTo(201);
        var saved = documents.findAll();
        assertThat(saved).hasSize(1);
        assertThat(saved.getFirst().getTitle()).isEqualTo("notes.pdf");
        assertThat(response.body()).contains("\"id\":" + saved.getFirst().getId(), "\"title\":\"notes.pdf\"");
        assertThat(saved.getFirst().getObjectKey()).startsWith("documents/").endsWith(".pdf");
        verify(rabbit).convertAndSend("document.uploaded", "Document created: "
                + saved.getFirst().getId() + " with key: " + saved.getFirst().getObjectKey());
    }

    @Test
    void pdfUploadUsesOptionalTitle() throws Exception {
        var response = upload(PDF_HEADER, "  Course notes  ");
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.body()).contains("\"title\":\"Course notes\"");
        assertThat(documents.findAll().getFirst().getTitle()).isEqualTo("Course notes");
    }

    @Test
    void missingEmptyAndNonPdfFilesAreRejectedWithoutSavingOrPublishing() throws Exception {
        for (byte[] file : new byte[][]{null, new byte[0], "not a PDF".getBytes(StandardCharsets.UTF_8)}) {
            assertThat(upload(file, "Invalid upload").statusCode()).isEqualTo(400);
        }
        assertThat(documents.count()).isZero();
        verify(rabbit, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void invalidUploadTitlesAreRejectedWithoutSavingOrPublishing() throws Exception {
        assertThat(upload(PDF_HEADER, " ").statusCode()).isEqualTo(400);
        assertThat(upload(PDF_HEADER, "x".repeat(256)).statusCode()).isEqualTo(400);
        assertThat(documents.count()).isZero();
        verify(rabbit, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void publicationFailureReturnsErrorAndRollsBackDocument() throws Exception {
        doThrow(new AmqpException("private broker details")).when(rabbit).convertAndSend(anyString(), any(Object.class));
        var response = upload(PDF_HEADER, null);
        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.body()).doesNotContain("private broker details");
        assertThat(documents.count()).isZero();
    }
}
