package at.technikum.swen3.summary;

import com.openai.errors.OpenAIIoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import tools.jackson.databind.json.JsonMapper;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class SummaryListenerTest {
    private final JsonMapper json = JsonMapper.builder().build();
    private final SummaryResultPublisher publisher = mock(SummaryResultPublisher.class);
    private final SummaryService service = mock(SummaryService.class);
    private final SummaryListener listener = new SummaryListener(
            LoggerFactory.getLogger(SummaryListener.class), publisher, service);

    @Test
    void providerFailureRejectsOneJobAndTheNextDocumentStillSucceeds(CapturedOutput output) {
        String firstText = "PRIVATE_OCR_FIRST";
        String secondText = "PRIVATE_OCR_SECOND";
        String privateSummary = "PRIVATE_MODEL_OUTPUT";
        String fakeKey = "PRIVATE_KEY_FROM_PROVIDER_ERROR";
        when(service.summarize(firstText))
                .thenThrow(new OpenAIIoException(fakeKey, new SocketTimeoutException("Read timed out")));
        when(service.summarize(secondText)).thenReturn(privateSummary);

        assertThatThrownBy(() -> listener.receiveText(ocrMessage(41, firstText)))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasMessageContaining("41")
                .hasNoCause();
        listener.receiveText(ocrMessage(42, secondText));

        verify(publisher).publish(new SummaryResult(41, null, true));
        verify(publisher).publish(new SummaryResult(42, privateSummary, false));
        assertThat(output.getAll())
                .contains("started: documentId=41", "failed: documentId=41", "succeeded: documentId=42")
                .doesNotContain(firstText, secondText, privateSummary, fakeKey);
    }

    @Test
    void blankOcrReportsFailureWithoutCallingOpenAi() {
        assertThatThrownBy(() -> listener.receiveText(ocrMessage(41, "  ")))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);

        verify(publisher).publish(new SummaryResult(41, null, true));
        verifyNoInteractions(service);
    }

    private Message ocrMessage(long id, String text) {
        return new Message(json.writeValueAsBytes(new OcrResult(id, text)));
    }
}
