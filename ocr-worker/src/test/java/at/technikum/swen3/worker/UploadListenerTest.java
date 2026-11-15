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
    private final OcrProcessor processor = mock(OcrProcessor.class);
    private final UploadListener listener = new UploadListener(logger, processor);

    @Test
    void storedDocumentStartsOcrWithItsIdAndKey() throws Exception {
        listener.receiveUpload(message("Document created: 42 with key: documents/abc_HelloWorld.pdf".getBytes(StandardCharsets.UTF_8)));
        verify(processor).process(42, "documents/abc_HelloWorld.pdf");
    }

    @Test
    void metadataOnlyMessageDoesNotTryToFetchAPdf() {
        listener.receiveUpload(message("Document created: 42".getBytes(StandardCharsets.UTF_8)));
        verifyNoInteractions(processor);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Document created: 0 with key: documents/a.pdf",
            "Document created: 42 with key: ", "Document created: 9999999999999999999999 with key: a.pdf"})
    void malformedStoredDocumentMessagesAreRejected(String body) {
        assertThatThrownBy(() -> listener.receiveUpload(message(body.getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(InvalidUploadMessageException.class);
        verifyNoInteractions(processor);
    }

    @Test
    void processingFailureIsRejectedForTheFailedQueue() throws Exception {
        doThrow(new java.io.IOException("Failed to read PDF")).when(processor).process(42, "documents/a.pdf");
        assertThatThrownBy(() -> listener.receiveUpload(message("Document created: 42 with key: documents/a.pdf".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(InvalidUploadMessageException.class).hasMessage("OCR processing failed");
    }

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
