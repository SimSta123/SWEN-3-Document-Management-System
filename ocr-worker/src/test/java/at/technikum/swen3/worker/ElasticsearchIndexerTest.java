package at.technikum.swen3.worker;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.*;

class ElasticsearchIndexerTest {
    private HttpServer server;
    private ElasticsearchIndexer indexer;
    private final List<Request> requests = new ArrayList<>();
    private int createStatus = 200;
    private String createBody = "{\"acknowledged\":true}";
    private int writeStatus = 201;
    private final JsonMapper json = JsonMapper.builder().build();

    record Request(String method, String path, String query, String authorization, String body) {}

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.add(new Request(exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
                    exchange.getRequestURI().getQuery(), exchange.getRequestHeaders().getFirst("Authorization"),
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
            boolean create = exchange.getRequestURI().getPath().equals("/documents");
            byte[] response = (create ? createBody : "{\"result\":\"created\"}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(create ? createStatus : writeStatus, response.length);
            try (var output = exchange.getResponseBody()) {
                output.write(response);
            }
        });
        server.start();
        indexer = new ElasticsearchIndexer("http://127.0.0.1:" + server.getAddress().getPort(),
                "documents", "elastic", "test-password");
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void createsMappingAndStoresEscapedTextUnderTheDocumentId() throws Exception {
        indexer.index(new OcrResult(42, "Hello \"World\"\nGrüße"));
        assertThat(requests).hasSize(2);
        var mapping = json.readTree(requests.getFirst().body());
        assertThat(mapping.path("mappings").path("properties").path("text").path("type").asString()).isEqualTo("text");
        assertThat(mapping.path("mappings").path("properties").path("documentId").path("type").asString()).isEqualTo("long");
        Request write = requests.get(1);
        assertThat(write.method()).isEqualTo("PUT");
        assertThat(write.path()).isEqualTo("/documents/_doc/42");
        assertThat(write.query()).isEqualTo("refresh=wait_for");
        assertThat(write.authorization()).isEqualTo("Basic " + Base64.getEncoder()
                .encodeToString("elastic:test-password".getBytes(StandardCharsets.UTF_8)));
        assertThat(json.readTree(write.body()).path("text").asString()).isEqualTo("Hello \"World\"\nGrüße");
        assertThat(json.readTree(write.body()).path("documentId").asLong()).isEqualTo(42);
    }

    @Test
    void repeatedDeliveryWritesTheSameIdWithoutRecreatingTheIndex() throws Exception {
        indexer.index(new OcrResult(42, "First text"));
        writeStatus = 200;
        indexer.index(new OcrResult(42, "Updated text"));
        assertThat(requests).hasSize(3);
        assertThat(requests.get(1).path()).isEqualTo(requests.get(2).path());
        assertThat(json.readTree(requests.get(2).body()).path("text").asString()).isEqualTo("Updated text");
    }

    @Test
    void anExistingIndexCanBeReusedAfterRestart() throws Exception {
        createStatus = 400;
        createBody = "{\"error\":{\"type\":\"resource_already_exists_exception\"}}";
        indexer.index(new OcrResult(42, "Hello"));
        assertThat(requests).hasSize(2);
    }

    @Test
    void otherIndexCreationErrorsAreNotTreatedAsSuccess() {
        createStatus = 400;
        createBody = "{\"error\":{\"type\":\"mapper_parsing_exception\"}}";
        assertThatThrownBy(() -> indexer.index(new OcrResult(42, "Hello")))
                .isInstanceOf(IOException.class).hasMessageContaining("index creation failed with status 400");
        assertThat(requests).hasSize(1);
    }

    @Test
    void failedWritesAreReported() {
        writeStatus = 503;
        assertThatThrownBy(() -> indexer.index(new OcrResult(42, "Hello")))
                .isInstanceOf(IOException.class).hasMessageContaining("indexing failed with status 503");
    }

    @Test
    void unavailableServerIsReported() {
        server.stop(0);
        assertThatThrownBy(() -> indexer.index(new OcrResult(42, "Hello"))).isInstanceOf(IOException.class);
    }

    @Test
    void invalidResultIsRejectedBeforeAnyRequest() {
        assertThatThrownBy(() -> indexer.index(new OcrResult(0, "Hello"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> indexer.index(new OcrResult(42, null))).isInstanceOf(IllegalArgumentException.class);
        assertThat(requests).isEmpty();
    }
}
