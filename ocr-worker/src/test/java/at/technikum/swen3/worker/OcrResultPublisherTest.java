package at.technikum.swen3.worker;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OcrResultPublisherTest {
    @Test
    void publishesPersistentJsonWithEscapedTextAndDocumentId() {
        var rabbit = mock(RabbitTemplate.class);
        var publisher = new OcrResultPublisher(rabbit, "test.ocr.completed");
        String text = "Hello \"World\"\nGrüße";
        publisher.publish(42, text);

        var message = ArgumentCaptor.forClass(Message.class);
        verify(rabbit).send(eq("test.ocr.completed"), message.capture());
        var json = JsonMapper.builder().build().readTree(message.getValue().getBody());
        assertThat(json.get("documentId").asLong()).isEqualTo(42);
        assertThat(json.get("text").asString()).isEqualTo(text);
        assertThat(message.getValue().getMessageProperties().getContentType()).isEqualTo("application/json");
        assertThat(message.getValue().getMessageProperties().getDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
    }
}
