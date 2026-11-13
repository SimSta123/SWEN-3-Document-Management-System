package at.technikum.swen3.worker;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class IndexingListener {
    private final ElasticsearchIndexer indexer;
    private final Logger logger;
    private final JsonMapper json = JsonMapper.builder().build();

    public IndexingListener(ElasticsearchIndexer indexer, Logger logger) {
        this.indexer = indexer;
        this.logger = logger;
    }

    @RabbitListener(queues = "${paperless.ocr.result-queue}")
    public void receiveResult(Message message) {
        OcrResult result;
        try {
            String body = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(message.getBody())).toString();
            var node = json.readTree(body);
            if (node == null || !node.path("documentId").isIntegralNumber()
                    || !node.path("documentId").canConvertToLong() || node.path("documentId").asLong() <= 0
                    || !node.path("text").isString()) {
                throw new IllegalArgumentException("OCR result needs a positive document id and text");
            }
            result = new OcrResult(node.get("documentId").asLong(), node.get("text").asString());
        } catch (IOException | RuntimeException exception) {
            logger.warn("Rejected invalid OCR result");
            throw new AmqpRejectAndDontRequeueException("Invalid OCR result", exception);
        }

        try {
            indexer.index(result);
            logger.info("Indexed OCR result: documentId={}, characters={}", result.documentId(), result.text().length());
        } catch (IOException | RuntimeException exception) {
            logger.error("Indexing failed: documentId={}", result.documentId(), exception);
            throw new AmqpRejectAndDontRequeueException("Elasticsearch indexing failed", exception);
        }
    }
}
