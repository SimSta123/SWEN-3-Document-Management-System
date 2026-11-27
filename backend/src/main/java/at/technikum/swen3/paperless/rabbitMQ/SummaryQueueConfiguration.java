package at.technikum.swen3.paperless.rabbitMQ;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SummaryQueueConfiguration {
    @Bean
    //Result AI Summary for Documents
    public Queue summaryResultQueue(@Value("${paperless.summary.result-queue}") String name) {
        return QueueBuilder.durable(name).build();
    }

    @Bean
    //To Summary Worker, for requesting Project AI Summary
    public Queue projectSummaryRequestQueue(@Value("${paperless.summary.project-request-queue}") String name) {
        return QueueBuilder.durable(name).build();
    }

    @Bean
    //From Summary Worker, to receive Project AI Summary
    public Queue projectSummaryResultQueue(@Value("${paperless.summary.project-result-queue}") String name) {
        return QueueBuilder.durable(name).build();
    }
}