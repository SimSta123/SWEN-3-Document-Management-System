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
import java.util.Base64;

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
        this.authorization = "Basic " + Base64.getEncoder()
                .encodeToString(
                        (username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    public void delete(Long documentId) throws IOException {
        if (documentId == null || documentId <= 0) {
            throw new IllegalArgumentException("Document ID must be positive");
        }

        var request = HttpRequest.newBuilder(
                        URI.create(indexUrl + "/_doc/" + documentId + "?refresh=wait_for"))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", authorization)
                .DELETE()
                .build();

        try {
            var response = client.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200 && response.statusCode() != 404) {
                throw new IOException(
                        "Elasticsearch deletion failed: " + response.statusCode());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Elasticsearch deletion interrupted", exception);
        }
    }
}