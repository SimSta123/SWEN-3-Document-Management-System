package at.technikum.swen3.worker;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class UploadListenerTest {
    private final Logger logger = mock(Logger.class);
    private final UploadListener listener = new UploadListener(logger);

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\r\n", "\t"})
    void blankMessagesAreRejected(String body) {
        assertThatThrownBy(() -> listener.receiveUpload(message(body.getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(InvalidUploadMessageException.class).hasMessage("Upload message must not be blank");
        verify(logger, never()).info(anyString(), any(Object.class));
    }

    @Test
    void brokenUtf8IsRejectedAndKeepsTheCause() {
        assertThatThrownBy(() -> listener.receiveUpload(message(new byte[]{(byte) 0xc3, 0x28})))
                .isInstanceOf(InvalidUploadMessageException.class)
                .hasMessage("Upload message must be UTF-8").hasCauseInstanceOf(java.nio.charset.CharacterCodingException.class);
    }

    @Test
    void validMessageStillWorksAfterARejectedMessage() {
        assertThatThrownBy(() -> listener.receiveUpload(message(new byte[0])))
                .isInstanceOf(InvalidUploadMessageException.class);
        listener.receiveUpload(message("{\"documentId\":42}".getBytes(StandardCharsets.UTF_8)));
        verify(logger).info("Received upload for OCR: {}", "{\"documentId\":42}");
    }

    @Test
    void lineBreaksStayOnOneLogLine() {
        listener.receiveUpload(message("notes\r\nnext line".getBytes(StandardCharsets.UTF_8)));
        verify(logger).info("Received upload for OCR: {}", "notes  next line");
    }

    private Message message(byte[] body) {
        return new Message(body, new MessageProperties());
    }
}
