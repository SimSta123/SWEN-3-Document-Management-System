package at.technikum.swen3.paperless.controller;


import at.technikum.swen3.paperless.service.DocumentService;
import at.technikum.swen3.paperless.service.exception.InvalidInputException;
import at.technikum.swen3.paperless.service.model.Document;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

@RestController
public class DocumentController {
    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    public record CreateDocumentRequest(@NotBlank @Size(max = 255) String title) {}

    @PostMapping(value = "/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public Document createDocument(@Valid @RequestBody CreateDocumentRequest request, HttpServletRequest httpRequest) {
        httpRequest.setAttribute("logMessage", "Requesting to create document with title: %s".formatted(request.title()));

        return service.createDocument(request.title());
    }

    // Sprint 3 accepts the PDF and starts the existing message flow.
    // Permanent file storage and OCR are added in Sprint 4.
    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Document uploadDocument(@RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String title, HttpServletRequest httpRequest) throws IOException {
        if (file.isEmpty()) {
            throw new InvalidInputException("PDF file must not be empty");
        }
        try (var input = file.getInputStream()) {
            if (!Arrays.equals(input.readNBytes(5), "%PDF-".getBytes(StandardCharsets.US_ASCII))) {
                throw new InvalidInputException("The uploaded file must have a PDF header");
            }
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            filename = "Uploaded PDF";
        }
        filename = filename.replace('\\', '/');
        filename = filename.substring(filename.lastIndexOf('/') + 1);
        httpRequest.setAttribute("logMessage", "Requesting to create document from PDF upload");
        return service.createActualDocument(file, title == null ? filename : title);
    }


    @GetMapping("/documents")
    public List<Document> listAllDocuments(@RequestParam(required = false) String search, HttpServletRequest httpRequest) throws IOException {
        httpRequest.setAttribute("logMessage", "Requesting to list all documents");

        if (search == null || search.isBlank()) {
            httpRequest.setAttribute("logMessage", "Requesting to list all documents");

            return service.listAllDocuments();
        }

        httpRequest.setAttribute("logMessage", "Requesting to search documents");

        return service.searchDocuments(search);
    }

    @GetMapping("/documents/{documentId}")
    public Document getDocument(@PathVariable Long documentId, HttpServletRequest httpRequest) {
        httpRequest.setAttribute("logMessage", "Requesting to get document with id: %s".formatted(documentId));

        return service.getDocument(documentId);
    }

    @GetMapping("/documents/{documentId}/pdf")
    public ResponseEntity<ByteArrayResource> openPdf(@PathVariable Long documentId, HttpServletRequest httpRequest)
            throws IOException {
        httpRequest.setAttribute("logMessage", "Requesting PDF for document id: %s".formatted(documentId));
        var pdf = service.getPdf(documentId);
        String filename = pdf.filename().replace('\\', '/');
        filename = filename.substring(filename.lastIndexOf('/') + 1).replaceAll("\\p{Cntrl}", "_").strip();
        if (filename.isEmpty()) {
            filename = "document.pdf";
        } else if (!filename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            filename += ".pdf";
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(filename, StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(new ByteArrayResource(pdf.bytes()));
    }

    @DeleteMapping("/documents/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(@PathVariable Long documentId, HttpServletRequest httpRequest) throws IOException {
        httpRequest.setAttribute("logMessage", "Requesting to delete document with id: %s".formatted(documentId));

        service.deleteDocument(documentId);
    }
}
