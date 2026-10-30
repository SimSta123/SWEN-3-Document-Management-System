package at.technikum.swen3.paperless.controller;


import at.technikum.swen3.paperless.service.DocumentService;
import at.technikum.swen3.paperless.service.ProjectService;
import at.technikum.swen3.paperless.service.mapping.ProjectMapper;
import at.technikum.swen3.paperless.service.model.Document;
import at.technikum.swen3.paperless.service.model.Project;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
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
        /*
        logger.info("Request: method={} path={} remoteAddress={}, requesting to create document with title: {}",
                httpRequest.getMethod(),
                httpRequest.getRequestURI(),
                httpRequest.getRemoteAddr(),
                request.title());
         */
        httpRequest.setAttribute("logMessage", "Requesting to create document with title: %s".formatted(request.title()));

        return service.createDocument(request.title());
    }

    @GetMapping("/documents")
    public List<Document> listAllDocuments(HttpServletRequest httpRequest) {
        /*
        logger.info("Request: method={} path={} remoteAddress={}, requesting a list of all documents",
                httpRequest.getMethod(),
                httpRequest.getRequestURI(),
                httpRequest.getRemoteAddr());
         */
        httpRequest.setAttribute("logMessage", "Requesting to list all documents");

        return service.listAllDocuments();
    }

    @GetMapping("/documents/{documentId}")
    public Document getDocument(@PathVariable Long documentId, HttpServletRequest httpRequest) {
        /*
        logger.info("Request: method={} path={} remoteAddress={}, requesting to get document with id: {}",
                httpRequest.getMethod(),
                httpRequest.getRequestURI(),
                httpRequest.getRemoteAddr(),
                documentId);
         */
        httpRequest.setAttribute("logMessage", "Requesting to get document with id: %s".formatted(documentId));

        return service.getDocument(documentId);
    }
}
