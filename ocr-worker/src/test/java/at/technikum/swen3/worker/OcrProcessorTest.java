package at.technikum.swen3.worker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OcrProcessorTest {
    private final PdfStorage storage = mock(PdfStorage.class);
    private final PdfOcr ocr = mock(PdfOcr.class);
    private final OcrResultPublisher results = mock(OcrResultPublisher.class);
    private final OcrProcessor processor = new OcrProcessor(storage, ocr, results, mock(Logger.class));

    @Test
    void fetchedPdfIsRecognizedAndPublishedWithTheSameDocumentId() throws Exception {
        var downloadedFile = new AtomicReference<Path>();
        doAnswer(call -> {
            Path target = call.getArgument(1);
            Files.writeString(target, "PDF fixture");
            downloadedFile.set(target);
            return null;
        }).when(storage).download(eq("documents/a.pdf"), any(Path.class));
        when(ocr.extract(any(Path.class))).thenReturn("Hello World");

        processor.process(42, "documents/a.pdf");

        verify(ocr).extract(downloadedFile.get());
        verify(results).publish(42, "Hello World");
        assertThat(downloadedFile.get()).doesNotExist();
        assertThat(downloadedFile.get().getParent()).doesNotExist();
    }

    @Test
    void failedDownloadDoesNotRunOcrOrPublishAResult() throws Exception {
        doThrow(new IOException("Storage unavailable")).when(storage).download(anyString(), any(Path.class));
        assertThatThrownBy(() -> processor.process(42, "a.pdf")).isInstanceOf(IOException.class);
        verifyNoInteractions(ocr, results);
    }

    @Test
    void failedOcrDoesNotPublishAResultAndRemovesTheDownloadedPdf() throws Exception {
        var downloadedFile = new AtomicReference<Path>();
        doAnswer(call -> {
            Path target = call.getArgument(1);
            Files.writeString(target, "PDF fixture");
            downloadedFile.set(target);
            return null;
        }).when(storage).download(anyString(), any(Path.class));
        when(ocr.extract(any(Path.class))).thenThrow(new IOException("OCR failed"));
        assertThatThrownBy(() -> processor.process(42, "a.pdf")).isInstanceOf(IOException.class);
        verifyNoInteractions(results);
        assertThat(downloadedFile.get().getParent()).doesNotExist();
    }
}
