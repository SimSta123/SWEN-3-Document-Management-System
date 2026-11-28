package at.technikum.swen3.paperless.rabbitMQ;

import at.technikum.swen3.paperless.repository.DocumentRepository;
import at.technikum.swen3.paperless.service.DocumentService;
import at.technikum.swen3.paperless.service.ProjectSummaryService;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

@Component
public class SummaryResultListener {
    private final JsonMapper json = JsonMapper.builder().build();
    private final DocumentService documentService;
    private final DocumentRepository documents;
    private final ProjectSummaryService projectSummaries;

    public SummaryResultListener(DocumentService documentService, DocumentRepository documents, ProjectSummaryService projectSummaries) {
        this.documentService = documentService;
        this.documents = documents;
        this.projectSummaries = projectSummaries;
    }

    @RabbitListener(queues = "${paperless.summary.result-queue}")
    public void receive(Message message) {
        SummaryResult result;
        try {
            result = json.readValue(message.getBody(), SummaryResult.class);
        } catch (JacksonException exception) {
            throw new AmqpRejectAndDontRequeueException("Invalid summary JSON", exception);
        }

        if (result == null || result.documentId() <= 0) {
            throw new AmqpRejectAndDontRequeueException("Invalid summary result");
        }
        boolean hasSummary = result.summary() != null && !result.summary().isBlank();
        boolean failed = Boolean.TRUE.equals(result.failed());
        if (hasSummary == failed) {
            throw new AmqpRejectAndDontRequeueException("Expected either a document summary or a failure");
        }

        if (failed) {
            documentService.markSummaryFailed(result.documentId());
        } else {
            documentService.saveSummary(result.documentId(), result.summary());
        }

        documents.findProjectIdByDocumentId(result.documentId()).ifPresent(projectSummaries::tryQueue);
    }

    @RabbitListener(queues = "${paperless.summary.project-result-queue}")
    public void receiveProjectSummary(Message message) {
        ProjectSummaryResult result;
        try {
            result = json.readValue(message.getBody(), ProjectSummaryResult.class);
        } catch (JacksonException exception) {
            throw new AmqpRejectAndDontRequeueException(
                    "Invalid project summary result JSON", exception);
        }

        if (result == null || result.projectId() <= 0 || result.requestId() == null) {
            throw new AmqpRejectAndDontRequeueException("Invalid project summary result");
        }

        boolean hasSummary = result.summary() != null && !result.summary().isBlank();
        boolean hasError = result.errorMessage() != null && !result.errorMessage().isBlank();

        if (hasSummary == hasError) {
            throw new AmqpRejectAndDontRequeueException(
                    "Expected either a project summary or an error");
        }

        projectSummaries.applyResult(result);
    }

    private record SummaryResult(long documentId, String summary, Boolean failed) {}
    public record ProjectSummaryResult(long projectId, UUID requestId, String summary, String errorMessage) {}
}