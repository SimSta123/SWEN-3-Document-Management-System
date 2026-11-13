package at.technikum.swen3.worker;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ElasticsearchIndexer {
    private static final String INDEX_MAPPING = """
            {"settings":{"number_of_shards":1,"number_of_replicas":0},
             "mappings":{"properties":{"documentId":{"type":"long"},"text":{"type":"text"}}}}
            """;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final JsonMapper json = JsonMapper.builder().build();
    private final String indexUrl;
    private final String authorization;
    private boolean indexReady;

    public ElasticsearchIndexer(@Value("${elasticsearch.url}") String url,
            @Value("${elasticsearch.index}") String index,
            @Value("${elasticsearch.username}") String username,
            @Value("${elasticsearch.password}") String password) {
        if (!index.matches("[a-z0-9][a-z0-9_-]{0,254}")) {
            throw new IllegalArgumentException("Invalid Elasticsearch index name");
        }
        this.indexUrl = url.replaceAll("/+$", "") + "/" + index;
        this.authorization = "Basic " + Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    public void index(OcrResult result) throws IOException {
        if (result.documentId() <= 0 || result.text() == null) {
            throw new IllegalArgumentException("OCR result needs a positive document id and text");
        }
        try {
            ensureIndex();
            var response = put(indexUrl + "/_doc/" + result.documentId() + "?refresh=wait_for",
                    json.writeValueAsString(result));
            if (response.statusCode() != 200 && response.statusCode() != 201) {
                throw new IOException("Elasticsearch indexing failed with status " + response.statusCode());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Elasticsearch indexing was interrupted", exception);
        }
    }

    private synchronized void ensureIndex() throws IOException, InterruptedException {
        if (indexReady) return;
        var response = put(indexUrl, INDEX_MAPPING);
        if (response.statusCode() != 200) {
            boolean alreadyExists = response.statusCode() == 400
                    && "resource_already_exists_exception".equals(json.readTree(response.body()).path("error").path("type").asString());
            if (!alreadyExists) {
                throw new IOException("Elasticsearch index creation failed with status " + response.statusCode());
            }
        }
        indexReady = true;
    }

    private HttpResponse<String> put(String url, String body) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", authorization)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
