package at.technikum.swen3.paperless.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ElasticsearchClient {
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final String indexUrl;
    private final String authorization;

    public ElasticsearchClient(
            @Value("${elasticsearch.url}") String url,
            @Value("${elasticsearch.index}") String index,
            @Value("${elasticsearch.username}") String username,
            @Value("${elasticsearch.password}") String password) {
        this.indexUrl = url.replaceAll("/+$", "") + "/" + index;
        this.authorization = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    public void delete(Long documentId) throws IOException {
        if (documentId == null || documentId <= 0) {
            throw new IllegalArgumentException("Document ID must be positive");
        }

        var request = HttpRequest.newBuilder(URI.create(indexUrl + "/_doc/" + documentId + "?refresh=wait_for"))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", authorization)
                .DELETE()
                .build();

        try {
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200 && response.statusCode() != 404) {
                throw new IOException(
                        "Elasticsearch deletion failed: " + response.statusCode());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Elasticsearch deletion interrupted", exception);
        }
    }

    public List<Long> search(String term) throws IOException {
        if (term == null || term.isBlank()) {
            return List.of();
        }

        var json = JsonMapper.builder().build();

        String body = json.writeValueAsString(Map.of(
                "size", 100,
                "_source", List.of("documentId"),
                "query", Map.of(
                        "match", Map.of("text", term.strip())
                )
        ));

        var request = HttpRequest.newBuilder(URI.create(indexUrl + "/_search"))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", authorization)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            var response = client.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                var error = json.readTree(response.body()).path("error");
                if ("index_not_found_exception".equals(
                        error.path("type").asString())) {
                    return List.of();
                }
            }

            if (response.statusCode() != 200) {
                throw new IOException(
                        "Elasticsearch search failed: " + response.statusCode());
            }

            var hits = json.readTree(response.body())
                    .path("hits")
                    .path("hits");

            List<Long> documentIds = new ArrayList<>();

            for (var hit : hits) {
                long documentId = hit.path("_source")
                        .path("documentId")
                        .asLong();

                if (documentId > 0) {
                    documentIds.add(documentId);
                }
            }

            return documentIds;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException(
                    "Elasticsearch search interrupted", exception);
        }
    }
}