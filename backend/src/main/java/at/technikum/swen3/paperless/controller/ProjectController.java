package at.technikum.swen3.paperless.controller;

import at.technikum.swen3.paperless.service.ProjectService;
import at.technikum.swen3.paperless.service.model.Document;
import at.technikum.swen3.paperless.service.model.Project;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ProjectController {
    private final ProjectService service;

    public ProjectController(ProjectService service) {
        this.service = service;
    }

    public record CreateProjectRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 2000) String description) {}

    public record CreateDocumentRequest(@NotBlank @Size(max = 255) String title) {}

    @PostMapping("/projects")
    @ResponseStatus(HttpStatus.CREATED)
    public Project createProject(@Valid @RequestBody CreateProjectRequest request) {
        return service.createProject(request.name(), request.description());
    }

    @GetMapping("/projects")
    public List<Project> listProjects() {
        return service.listProjects();
    }

    @PostMapping("/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public Document createDocument(@Valid @RequestBody CreateDocumentRequest request) {
        return service.createDocument(request.title());
    }

    @PutMapping("/projects/{projectId}/documents/{documentId}")
    public Document assignDocument(@PathVariable Long projectId, @PathVariable Long documentId) {
        return service.assignDocument(projectId, documentId);
    }

    @GetMapping("/projects/{projectId}/documents")
    public List<Document> listDocuments(@PathVariable Long projectId) {
        return service.listDocuments(projectId);
    }
}
