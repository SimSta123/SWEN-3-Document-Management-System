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
    private final String projectQueue;
    private final JsonMapper json = JsonMapper.builder().build();
    private final Logger logger;


    public SummaryResultPublisher(RabbitTemplate rabbit, @Value("${paperless.summary.result-queue}") String queue,
                                  @Value("${paperless.summary.project-result-queue}") String projectQueue, Logger logger) {
        this.rabbit = rabbit;
        this.queue = queue;
        this.projectQueue = projectQueue;
        this.logger = logger;
    }

    public void publish(SummaryResult result) {
        logger.info("Sending document summary to {}", queue);
        send(queue, result);
    }

    public void publish(ProjectSummaryResult result) {
        send(projectQueue, result);
    }

    private void send(String destination, Object result) {
        var message = MessageBuilder.withBody(json.writeValueAsBytes(result))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setContentEncoding("UTF-8")
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .build();

        rabbit.send("", destination, message);
    }
}
