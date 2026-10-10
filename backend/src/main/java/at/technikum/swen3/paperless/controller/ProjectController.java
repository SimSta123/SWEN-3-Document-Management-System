package at.technikum.swen3.paperless.controller;

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

import java.util.List;

@RestController
public class ProjectController {
    private final ProjectService service;

    private final Logger logger;

    public ProjectController(ProjectService service, Logger logger) {
        this.service = service; this.logger = logger;
    }


    public record CreateProjectRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 2000) String description) {}

    public record CreateDocumentRequest(@NotBlank @Size(max = 255) String title) {}

    @PostMapping("/projects")
    @ResponseStatus(HttpStatus.CREATED)
    public Project createProject(@Valid @RequestBody CreateProjectRequest request) {
        logger.info(request.toString());
        return service.createProject(request.name(), request.description());
    }

    @GetMapping("/projects")
    public List<Project> listProjects() {
        logger.info("Requesting listing of projects");
        return service.listProjects();
    }

    @PostMapping("/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public Document createDocument(@Valid @RequestBody CreateDocumentRequest request) {
        logger.info(request.toString());
        return service.createDocument(request.title());
    }

    @PutMapping("/projects/{projectId}/documents/{documentId}")
    public Document assignDocument(@PathVariable Long projectId, @PathVariable Long documentId) {
        logger.info("Assigning document: {} to project: {}", documentId, projectId);
        return service.assignDocument(projectId, documentId);
    }

    @GetMapping("/documents")
    public List<Document> listAllDocuments() {
        logger.info("Requesting listing all documents");
        return service.listAllDocuments();
    }

    @GetMapping("/documents/{documentId}")
    public Document getDocument(@PathVariable Long documentId) {
        logger.info("Requesting document: {}", documentId);
        return service.getDocument(documentId);
    }

    @GetMapping("/projects/{projectId}/documents")
    public List<Document> listDocuments(@PathVariable Long projectId) {
        logger.info("Requesting listing documents for project: {}", projectId);
        return service.listDocuments(projectId);
    }
}
