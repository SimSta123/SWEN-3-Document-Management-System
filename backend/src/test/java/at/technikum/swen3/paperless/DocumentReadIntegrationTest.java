package at.technikum.swen3.paperless;

import at.technikum.swen3.paperless.repository.DocumentRepository;
import at.technikum.swen3.paperless.repository.ProjectRepository;
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

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

// Check controller, service, mapper and persistence together using real HTTP.
// https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DocumentReadIntegrationTest {
    @LocalServerPort private int port;
    @Autowired private ProjectService service;
    @Autowired private DocumentRepository documents;
    @Autowired private ProjectRepository projects;

    @BeforeEach
    void clearRecords() {
        documents.deleteAll();
        projects.deleteAll();
    }

    @MockitoBean
    private RabbitTemplate rabbit;

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
        var unassigned = service.createDocument("Unassigned notes");
        var assigned = service.createDocument("Project notes");
        var project = service.createProject("University", "");
        service.assignDocument(project.id(), assigned.id());
        var response = request("GET", "/documents", null);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains(
                "{\"id\":" + unassigned.id() + ",\"title\":\"Unassigned notes\",\"projectId\":null}",
                "{\"id\":" + assigned.id() + ",\"title\":\"Project notes\",\"projectId\":" + project.id() + "}");
    }

    @Test
    void emptyLibraryReturnsEmptyArray() throws Exception {
        var response = request("GET", "/documents", null);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("[]");
    }

    @Test
    void detailReturnsSelectedRecordAndProject() throws Exception {
        var document = service.createDocument("Selected document");
        var project = service.createProject("Course", "");
        service.assignDocument(project.id(), document.id());
        var response = request("GET", "/documents/" + document.id(), null);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("{\"id\":" + document.id()
                + ",\"title\":\"Selected document\",\"projectId\":" + project.id() + "}");
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
}
