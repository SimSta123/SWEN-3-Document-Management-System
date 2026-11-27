package at.technikum.swen3.summary;

import org.slf4j.Logger;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.stream.Collectors;

@Component
public class SummaryListener {
    private final JsonMapper json = JsonMapper.builder().build();
    private final Logger logger;
    private final SummaryResultPublisher publisher;
    private final SummaryService summaryService;

    public SummaryListener(Logger logger, SummaryResultPublisher publisher, SummaryService summaryService) {
        this.logger = logger;
        this.publisher = publisher;
        this.summaryService = summaryService;
    }

    @RabbitListener(queues = "${paperless.summary.request-queue}")
    public void receiveText(Message message) {
        OcrResult result = json.readValue(message.getBody(), OcrResult.class);
        logger.info("got from documentId: {}, Text: {}", result.documentId(), result.text());

        if (result == null || result.documentId() <= 0 || result.text() == null) {
            throw new AmqpRejectAndDontRequeueException("Invalid OCR result");
        }

        if (result.text().isBlank()) {
            return;
        }

        String summary = summaryService.summarize(result.text());
        logger.info("summary: {}", summary);
        publisher.publish(new SummaryResult(result.documentId(), summary));
    }

    @RabbitListener(queues = "${paperless.summary.project-request-queue}")
    public void receive(Message message) {
        ProjectSummaryRequest request;
        // Ungültige JSON-Nachrichten ablehnen, da eine erneute Zustellung sie nicht repariert.
        try {
            request = json.readValue(message.getBody(), ProjectSummaryRequest.class);
        } catch (JacksonException exception) {
            throw new AmqpRejectAndDontRequeueException("Invalid project summary JSON", exception);
        }

        // IDs für die Zuordnung und verwendbare Quellen für den AI Aufruf prüfen
        if (request == null || request.projectId() <= 0 || request.requestId() == null
                || request.sources() == null || request.sources().isEmpty()
                || request.sources().stream().anyMatch(source ->
                source == null || source.title() == null
                        || source.summary() == null || source.summary().isBlank())) {
            throw new AmqpRejectAndDontRequeueException("Invalid project summary request");
        }

        // Titel und Summary pro Dokument zusammenhalten und die Dokumente klar trennen.
        String sources = request.sources().stream()
                .map(source -> source.title() + "\n" + source.summary())
                .collect(Collectors.joining("\n\n"));

        ProjectSummaryResult result;
        try {
            String summary = summaryService.summarizeProject(sources);
            result = new ProjectSummaryResult(
                    request.projectId(), request.requestId(), summary, null);
        } catch (RuntimeException exception) {
            logger.warn("Project summary failed for request {} ({})",
                    request.requestId(), exception.getClass().getSimpleName());
            result = new ProjectSummaryResult(
                    request.projectId(), request.requestId(), null,
                    "The project summary could not be generated.");
        }

        publisher.publish(result);
    }

}
