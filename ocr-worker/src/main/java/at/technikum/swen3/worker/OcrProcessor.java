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
    private final DocumentLifecycle documents;

    public OcrProcessor(PdfStorage storage, PdfOcr ocr, OcrResultPublisher results, Logger logger,
            DocumentLifecycle documents) {
        this.storage = storage;
        this.ocr = ocr;
        this.results = results;
        this.logger = logger;
        this.documents = documents;
    }

    public void process(long documentId, String key) throws IOException {
        if (!documents.exists(documentId)) {
            logger.info("Skipped OCR for deleted document: documentId={}", documentId);
            return;
        }
        var directory = Files.createTempDirectory("paperless-ocr-document-");
        try {
            var pdf = directory.resolve("document.pdf");
            storage.download(key, pdf);
            String text = ocr.extract(pdf);
            if (!documents.exists(documentId)) {
                logger.info("Discarded OCR for deleted document: documentId={}", documentId);
                return;
            }
            results.publish(documentId, text);
            logger.info("OCR completed: documentId={}, characters={}", documentId, text.length());
        } catch (IOException | RuntimeException exception) {
            // A delete can remove the original PDF during download or OCR. That is a stale
            // job, not a broken document. Database failures still propagate (fail closed).
            if (documents.exists(documentId)) throw exception;
            logger.info("Discarded failed OCR for deleted document: documentId={}", documentId);
        } finally {
            PdfOcr.deleteDirectory(directory);
        }
    }
}
