package at.technikum.swen3.paperless.controller;


import at.technikum.swen3.paperless.service.DocumentService;
import at.technikum.swen3.paperless.service.model.Document;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

@RestController
public class DocumentController {
    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    public record CreateDocumentRequest(@NotBlank @Size(max = 255) String title) {}

    @PostMapping("/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public Document createDocument(@Valid @RequestBody ProjectController.CreateDocumentRequest request, HttpServletRequest httpRequest) {
        httpRequest.setAttribute("logMessage", "Requesting to create document with title: %s".formatted(request.title()));

        return service.createDocument(request.title());
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
