package at.technikum.swen3.paperless.controller;


import at.technikum.swen3.paperless.service.DocumentService;
import at.technikum.swen3.paperless.service.exception.InvalidInputException;
import at.technikum.swen3.paperless.service.model.Document;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@RestController
public class DocumentController {
    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    public record CreateDocumentRequest(@NotBlank @Size(max = 255) String title) {}

    @PostMapping(value = "/documents", consumes = MediaType.APPLICATION_JSON_VALUE)
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
        return service.createDocument(title == null ? filename : title);
    }

    @GetMapping("/documents")
    public List<Document> listAllDocuments(HttpServletRequest httpRequest) {
        httpRequest.setAttribute("logMessage", "Requesting to list all documents");

        return service.listAllDocuments();
    }

    @GetMapping("/documents/{documentId}")
    public Document getDocument(@PathVariable Long documentId, HttpServletRequest httpRequest) {
        httpRequest.setAttribute("logMessage", "Requesting to get document with id: %s".formatted(documentId));

        return service.getDocument(documentId);
    }
}
