package at.technikum.swen3.worker;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueueConfiguration {
    @Bean
    public Queue ocrResultQueue(@Value("${paperless.ocr.result-queue}") String name) {
        return QueueBuilder.durable(name).deadLetterExchange("").deadLetterRoutingKey(name + ".failed").build();
    }

    @Bean
    public Queue failedOcrResultQueue(@Value("${paperless.ocr.result-queue}") String name) {
        return QueueBuilder.durable(name + ".failed").build();
    }

    @Bean
    public Queue uploadQueue(@Value("${paperless.ocr.queue}") String name) {
        return QueueBuilder.durable(name).deadLetterExchange("").deadLetterRoutingKey(name + ".failed").build();
    }

    @Bean
    public Queue failedUploadQueue(@Value("${paperless.ocr.queue}") String name) {
        return QueueBuilder.durable(name + ".failed").build();
    }

    //Damit mit einen Queue aufruf in 2 Queues die Message schicken
    @Bean
    public FanoutExchange ocrExchange(@Value("${paperless.ocr.exchange}") String name) {
        //name: Name des Exchange
        return new FanoutExchange(name, true, false);
    }

    @Bean
    public Queue summaryRequestQueue(@Value("${paperless.summary.request-queue}") String name) {
        return queueWithDeadLetterRouting(name);
    }

    @Bean
    public Queue failedSummaryRequestQueue(@Value("${paperless.summary.request-queue}") String name) {
        return QueueBuilder.durable(name + ".failed").build();
    }

    //ocrResultQueue einbinden in die ocrExchangeQueue
    @Bean
    public Binding bindOcrResultQueue(@Qualifier("ocrResultQueue") Queue queue, FanoutExchange ocrExchange) {
        return BindingBuilder.bind(queue).to(ocrExchange);
    }

    //SummaryRequestQueue einbinden in die ocrExchangeQueue
    @Bean
    public Binding bindSummaryRequestQueue(@Qualifier("summaryRequestQueue") Queue queue, FanoutExchange ocrExchange) {
        return BindingBuilder.bind(queue).to(ocrExchange);
    }

    //Damit, falls die Queue von summaryRequest abgehlent wurde, sie in die Failed weitergeleitet werden kann
    private Queue queueWithDeadLetterRouting(String name) {
        return QueueBuilder.durable(name)//erstell die neue queue, kann neustart überlegen
                .deadLetterExchange("")//Standard exchange
                .deadLetterRoutingKey(name + ".failed")//Ziel
                .build();
    }
}
