package at.technikum.swen3.paperless.rabbitMQ;

import org.slf4j.Logger;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RabbitDocumentEventPublisher implements DocumentEventPublisher {

    private final RabbitTemplate rabbit;
    private final String uploadQueue;
    private final Logger logger;

    public RabbitDocumentEventPublisher(RabbitTemplate rabbit, @Value("${paperless.ocr.queue}") String uploadQueue, Logger logger) {
        this.rabbit = rabbit;
        // Spring synchronizes the AMQP commit after the surrounding database transaction.
        // Otherwise the worker could receive the ID before its record is visible.
        this.rabbit.setChannelTransacted(true);
        this.uploadQueue = uploadQueue;
        this.logger = logger;
    }

    @Override
    public void publishDocumentCreated(Long documentId) {
        rabbit.convertAndSend(uploadQueue, "Document created: " + documentId);

        logger.debug("Published document-created message: documentId={}, queue={}", documentId, uploadQueue);
    }

    @Override
    public void publishDocumentCreatedWKey(Long documentId, String key) {
        rabbit.convertAndSend(uploadQueue, "Document created: " + documentId + " with key: " + key);

        logger.debug("Published document-created message: documentId={}, key={}, queue={}", documentId, key, uploadQueue);
    }
}
