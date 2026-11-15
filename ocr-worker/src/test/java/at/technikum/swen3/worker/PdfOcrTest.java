package at.technikum.swen3.worker;

import java.awt.Color;
import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.*;

class PdfOcrTest {
    @TempDir Path directory;

    @Test
    void tesseractReadsEveryPageOfAnImageOnlyPdf() throws Exception {
        Path pdf = imagePdf("Hello World", "Second page");
        String text = new PdfOcr("tesseract", "eng", Duration.ofSeconds(60)).extract(pdf);
        assertThat(text).contains("Hello World", "Second page");
        assertThat(text.indexOf("Hello World")).isLessThan(text.indexOf("Second page"));
    }

    @Test
    void invalidPdfIsRejected() throws Exception {
        Path pdf = directory.resolve("invalid.pdf");
        Files.writeString(pdf, "%PDF-not-a-real-document");
        assertThatThrownBy(() -> new PdfOcr("tesseract", "eng", Duration.ofSeconds(60)).extract(pdf))
                .isInstanceOf(IOException.class);
    }

    @Test
    void pdfWithoutPagesIsRejected() throws Exception {
        Path pdf = directory.resolve("empty.pdf");
        try (var document = new PDDocument()) {
            document.save(pdf.toFile());
        }
        assertThatThrownBy(() -> new PdfOcr("tesseract", "eng", Duration.ofSeconds(60)).extract(pdf))
                .isInstanceOf(IOException.class).hasMessage("PDF has no pages");
    }

    @Test
    void missingTesseractDoesNotProduceASuccessfulResult() throws Exception {
        Path pdf = imagePdf("Hello World");
        assertThatThrownBy(() -> new PdfOcr("/missing-tesseract-command", "eng", Duration.ofSeconds(60)).extract(pdf))
                .isInstanceOf(IOException.class);
    }

    @Test
    void invalidLanguageReportsTheTesseractFailure() throws Exception {
        Path pdf = imagePdf("Hello World");
        assertThatThrownBy(() -> new PdfOcr("tesseract", "not-an-installed-language", Duration.ofSeconds(60)).extract(pdf))
                .isInstanceOf(IOException.class).hasMessageContaining("Tesseract failed with exit code");
    }

    private Path imagePdf(String... pages) throws IOException {
        Path pdf = directory.resolve("scan.pdf");
        try (var document = new PDDocument()) {
            for (String text : pages) {
                var image = new BufferedImage(1000, 200, BufferedImage.TYPE_INT_RGB);
                var graphics = image.createGraphics();
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
                graphics.setColor(Color.BLACK);
                graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 72));
                graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                graphics.drawString(text, 40, 130);
                graphics.dispose();
                var page = new PDPage();
                document.addPage(page);
                try (var content = new PDPageContentStream(document, page)) {
                    content.drawImage(LosslessFactory.createFromImage(document, image), 50, 600, 500, 100);
                }
            }
            document.save(pdf.toFile());
        }
        return pdf;
    }
}
