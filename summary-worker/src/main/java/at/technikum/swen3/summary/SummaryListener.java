package at.technikum.swen3.summary;

import org.slf4j.Logger;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

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

}
