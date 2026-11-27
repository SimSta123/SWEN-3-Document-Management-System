package at.technikum.swen3.summary;

import org.slf4j.Logger;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class SummaryResultPublisher {
    private final RabbitTemplate rabbit;
    private final String queue;
    private final JsonMapper json = JsonMapper.builder().build();
    private final Logger logger;


    public SummaryResultPublisher(RabbitTemplate rabbit, @Value("${paperless.summary.result-queue}") String queue, Logger logger) {
        this.rabbit = rabbit;
        this.queue = queue;
        this.logger = logger;
    }

    public void publish(SummaryResult result) {
        logger.info("Sending summary into document.genai.completed queue");
        var message = MessageBuilder.withBody(json.writeValueAsBytes(result))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setContentEncoding("UTF-8")
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .build();
        rabbit.send(queue, message);
    }
}
