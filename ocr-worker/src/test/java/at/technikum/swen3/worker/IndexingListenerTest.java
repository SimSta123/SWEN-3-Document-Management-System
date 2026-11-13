package at.technikum.swen3.worker;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class IndexingListenerTest {
    private final ElasticsearchIndexer indexer = mock(ElasticsearchIndexer.class);
    private final IndexingListener listener = new IndexingListener(indexer, mock(Logger.class));

    @Test
    void passesDocumentIdAndTextToTheIndexer() throws Exception {
        listener.receiveResult(message("{\"documentId\":42,\"text\":\"Hello World\\nGrüße\"}"));
        verify(indexer).index(new OcrResult(42, "Hello World\nGrüße"));
    }

    @Test
    void aBlankPageIsStillAValidOcrResult() throws Exception {
        listener.receiveResult(message("{\"documentId\":42,\"text\":\"\"}"));
        verify(indexer).index(new OcrResult(42, ""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "not-json", "null", "{}", "[]", "{\"documentId\":0,\"text\":\"Hello\"}",
            "{\"documentId\":42}", "{\"documentId\":42,\"text\":null}",
            "{\"documentId\":\"42\",\"text\":\"Hello\"}", "{\"documentId\":1.5,\"text\":\"Hello\"}",
            "{\"documentId\":99999999999999999999999,\"text\":\"Hello\"}"})
    void invalidResultsAreRejectedWithoutCallingElasticsearch(String body) {
        assertThatThrownBy(() -> listener.receiveResult(message(body)))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class).hasMessage("Invalid OCR result");
        verifyNoInteractions(indexer);
    }

    @Test
    void malformedUtf8IsRejected() {
        assertThatThrownBy(() -> listener.receiveResult(new Message(new byte[]{(byte) 0xc3, 0x28}, new MessageProperties())))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        verifyNoInteractions(indexer);
    }

    @Test
    void indexingFailureRejectsTheMessageForTheFailedQueue() throws Exception {
        doThrow(new IOException("Elasticsearch unavailable")).when(indexer).index(any(OcrResult.class));
        assertThatThrownBy(() -> listener.receiveResult(message("{\"documentId\":42,\"text\":\"Hello\"}")))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class).hasMessage("Elasticsearch indexing failed");
    }

    private Message message(String body) {
        return new Message(body.getBytes(StandardCharsets.UTF_8), new MessageProperties());
    }
}
