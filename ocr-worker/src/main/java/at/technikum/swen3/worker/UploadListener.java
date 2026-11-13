package at.technikum.swen3.worker;

import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.io.IOException;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class UploadListener {
    private static final Pattern STORED_DOCUMENT = Pattern.compile("Document created: ([1-9][0-9]*) with key: (\\S+)");
    private final Logger logger;
    private final OcrProcessor processor;

    public UploadListener(Logger logger, OcrProcessor processor) {
        this.logger = logger;
        this.processor = processor;
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
            var storedDocument = STORED_DOCUMENT.matcher(body);
            if (storedDocument.matches()) {
                long documentId;
                try {
                    documentId = Long.parseLong(storedDocument.group(1));
                } catch (NumberFormatException exception) {
                    throw new InvalidUploadMessageException("Document id is out of range", exception);
                }
                processor.process(documentId, storedDocument.group(2));
            } else if (body.contains(" with key:")) {
                throw new InvalidUploadMessageException("Stored document message is invalid");
            }
        } catch (CharacterCodingException exception) {
            logger.warn("Rejected upload message: invalid UTF-8");
            throw new InvalidUploadMessageException("Upload message must be UTF-8", exception);
        } catch (InvalidUploadMessageException exception) {
            logger.warn("Rejected upload message: {}", exception.getMessage());
            throw exception;
        } catch (IOException | RuntimeException exception) {
            logger.error("OCR processing failed", exception);
            throw new InvalidUploadMessageException("OCR processing failed", exception);
        }
    }
}
