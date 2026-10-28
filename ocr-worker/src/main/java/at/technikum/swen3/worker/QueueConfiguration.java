package at.technikum.swen3.worker;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueueConfiguration {
    @Bean
    public Queue uploadQueue(@Value("${paperless.ocr.queue}") String name) {
        return QueueBuilder.durable(name).deadLetterExchange("").deadLetterRoutingKey(name + ".failed").build();
    }

    @Bean
    public Queue failedUploadQueue(@Value("${paperless.ocr.queue}") String name) {
        return QueueBuilder.durable(name + ".failed").build();
    }
}
