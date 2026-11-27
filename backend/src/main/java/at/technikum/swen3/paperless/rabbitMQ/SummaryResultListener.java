package at.technikum.swen3.paperless.rabbitMQ;

import at.technikum.swen3.paperless.service.DocumentService;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
public class SummaryResultListener {
    private final JsonMapper json = JsonMapper.builder().build();
    private final DocumentService documentService;

    public SummaryResultListener(DocumentService documentService) {
        this.documentService = documentService;
    }

    @RabbitListener(queues = "${paperless.summary.result-queue}")
    public void receive(Message message) {
        SummaryResult result;
        try {
            result = json.readValue(message.getBody(), SummaryResult.class);
        } catch (JacksonException exception) {
            throw new AmqpRejectAndDontRequeueException("Invalid summary JSON", exception);
        }

        if (result == null || result.documentId() <= 0
                || result.summary() == null || result.summary().isBlank()) {
            throw new AmqpRejectAndDontRequeueException("Invalid summary result");
        }

        documentService.saveSummary(result.documentId(), result.summary());
    }

    private record SummaryResult(long documentId, String summary) {}
}