package at.technikum.swen3.worker;

import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class OcrResultPublisher {
    private final RabbitTemplate rabbit;
    private final String queue;
    private final JsonMapper json = JsonMapper.builder().build();

    public OcrResultPublisher(RabbitTemplate rabbit, @Value("${paperless.ocr.result-queue}") String queue) {
        this.rabbit = rabbit;
        this.queue = queue;
    }

    public void publish(long documentId, String text) {
        var message = MessageBuilder.withBody(json.writeValueAsBytes(new OcrResult(documentId, text)))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setContentEncoding("UTF-8")
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .build();
        rabbit.send(queue, message);
    }
}
