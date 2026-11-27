package at.technikum.swen3.paperless.rabbitMQ;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SummaryQueueConfiguration {
    @Bean
    public Queue summaryResultQueue(@Value("${paperless.summary.result-queue}") String name) {
        return QueueBuilder.durable(name).build();
    }
}