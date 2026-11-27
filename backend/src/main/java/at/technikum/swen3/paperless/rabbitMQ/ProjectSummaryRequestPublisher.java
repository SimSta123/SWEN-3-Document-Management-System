package at.technikum.swen3.paperless.rabbitMQ;

import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ProjectSummaryRequestPublisher {

    private final RabbitTemplate rabbit;
    private final String queue;
    private final JsonMapper json = JsonMapper.builder().build();

    public ProjectSummaryRequestPublisher(RabbitTemplate rabbit, @Value("${paperless.summary.project-request-queue}") String queue) {
        this.rabbit = rabbit;
        this.queue = queue;
    }

    public void publish(ProjectSummaryRequest request) {
        var message = MessageBuilder.withBody(json.writeValueAsBytes(request))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setContentEncoding("UTF-8")
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .build();

        rabbit.send("", queue, message);
    }
}