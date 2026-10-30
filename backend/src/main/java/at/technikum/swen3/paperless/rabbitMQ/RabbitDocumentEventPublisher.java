package at.technikum.swen3.paperless.rabbitMQ;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        this.uploadQueue = uploadQueue;
        this.logger = logger;
    }

    @Override
    public void publishDocumentCreated(Long documentId) {
        rabbit.convertAndSend(uploadQueue, "Document created: " + documentId);

        logger.debug("Published document-created message: documentId={}, queue={}", documentId, uploadQueue);
    }
}