package at.technikum.swen3.worker;

import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;

import org.slf4j.Logger;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class UploadListener {
    private final Logger logger;

    public UploadListener(Logger logger) {
        this.logger = logger;
    }

    // Spring declares the queue and calls this when a message arrives.
    // https://docs.spring.io/spring-amqp/reference/amqp/receiving-messages/async-annotation-driven.html
    @RabbitListener(queues = "${paperless.ocr.queue}")
    public void receiveUpload(Message message) {
        try {
            String body = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(message.getBody())).toString();
            if (body.isBlank()) {
                throw new InvalidUploadMessageException("Upload message must not be blank");
            }
            logger.info("Received upload for OCR: {}", body.replace('\r', ' ').replace('\n', ' '));
        } catch (CharacterCodingException exception) {
            logger.warn("Rejected upload message: invalid UTF-8");
            throw new InvalidUploadMessageException("Upload message must be UTF-8", exception);
        } catch (InvalidUploadMessageException exception) {
            logger.warn("Rejected upload message: {}", exception.getMessage());
            throw exception;
        }
    }
}
