package at.technikum.swen3.worker;

import java.io.IOException;
import java.nio.file.Files;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class OcrProcessor {
    private final PdfStorage storage;
    private final PdfOcr ocr;
    private final OcrResultPublisher results;
    private final Logger logger;

    public OcrProcessor(PdfStorage storage, PdfOcr ocr, OcrResultPublisher results, Logger logger) {
        this.storage = storage;
        this.ocr = ocr;
        this.results = results;
        this.logger = logger;
    }

    public void process(long documentId, String key) throws IOException {
        var directory = Files.createTempDirectory("paperless-ocr-document-");
        try {
            var pdf = directory.resolve("document.pdf");
            storage.download(key, pdf);
            String text = ocr.extract(pdf);
            results.publish(documentId, text);
            logger.info("OCR completed: documentId={}, characters={}", documentId, text.length());
        } finally {
            PdfOcr.deleteDirectory(directory);
        }
    }
}
