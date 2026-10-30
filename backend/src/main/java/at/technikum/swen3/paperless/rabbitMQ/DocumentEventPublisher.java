package at.technikum.swen3.paperless.rabbitMQ;

public interface DocumentEventPublisher {
    void publishDocumentCreated(Long documentId);
}
