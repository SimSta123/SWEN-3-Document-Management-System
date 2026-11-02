package at.technikum.swen3.worker;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;

// Bad messages go to the failed queue instead of being retried forever.
public class InvalidUploadMessageException extends AmqpRejectAndDontRequeueException {
    public InvalidUploadMessageException(String message) {
        super(message);
    }

    public InvalidUploadMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
