package at.technikum.swen3.worker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PdfOcr {
    private final String command;
    private final String language;
    private final Duration timeout;

    public PdfOcr(@Value("${paperless.ocr.command}") String command,
            @Value("${paperless.ocr.language}") String language,
            @Value("${paperless.ocr.timeout}") Duration timeout) {
        this.command = command;
        this.language = language;
        this.timeout = timeout;
    }

    public String extract(Path pdf) throws IOException {
        Path directory = Files.createTempDirectory("paperless-ocr-pages-");
        try (var document = Loader.loadPDF(pdf.toFile())) {
            if (document.getNumberOfPages() == 0) {
                throw new IOException("PDF has no pages");
            }
            var renderer = new PDFRenderer(document);
            var text = new StringBuilder();
            for (int page = 0; page < document.getNumberOfPages(); page++) {
                Path image = directory.resolve("page.png");
                var rendered = renderer.renderImageWithDPI(page, 300, ImageType.GRAY);
                try {
                    ImageIO.write(rendered, "png", image.toFile());
                } finally {
                    rendered.flush();
                }
                if (page > 0) text.append('\n');
                text.append(recognize(image, directory));
                Files.delete(image);
            }
            return text.toString();
        } finally {
            deleteDirectory(directory);
        }
    }

    private String recognize(Path image, Path directory) throws IOException {
        Path output = directory.resolve("result");
        Path errors = directory.resolve("tesseract.log");
        Process process = new ProcessBuilder(command, image.toString(), output.toString(), "-l", language, "--dpi", "300")
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(errors.toFile())
                .start();
        try {
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                throw new IOException("Tesseract timed out");
            }
            if (process.exitValue() != 0) {
                throw new IOException("Tesseract failed with exit code " + process.exitValue());
            }
            return Files.readString(directory.resolve("result.txt"));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("OCR was interrupted", exception);
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                try {
                    process.waitFor();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    static void deleteDirectory(Path directory) throws IOException {
        try (var files = Files.walk(directory)) {
            for (Path path : files.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
