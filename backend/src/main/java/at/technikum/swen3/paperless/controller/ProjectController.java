package at.technikum.swen3.paperless.controller;

import at.technikum.swen3.paperless.service.ProjectService;
import at.technikum.swen3.paperless.service.model.Document;
import at.technikum.swen3.paperless.service.model.Project;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

@RestController
public class ProjectController {
    private final ProjectService service;

    public ProjectController(ProjectService service) {
        this.service = service;
    }

    public record CreateProjectRequest(@NotBlank @Size(max = 200) String name, @Size(max = 2000) String description) {}

    @PostMapping("/projects")
    @ResponseStatus(HttpStatus.CREATED)
    public Project createProject(@Valid @RequestBody CreateProjectRequest request, HttpServletRequest httpRequest) {
        httpRequest.setAttribute("logMessage", "Requesting to create project with name: %s, description: %s".formatted(request.name(), request.description()));

        return service.createProject(request.name(), request.description());
    }

    @GetMapping("/projects")
    public List<Project> listProjects(HttpServletRequest httpRequest) {
        httpRequest.setAttribute("logMessage", "Requesting to list all projects");

        return service.listProjects();
    }


    @PutMapping("/projects/{projectId}/documents/{documentId}")
    public Document assignDocument(@PathVariable Long projectId, @PathVariable Long documentId, HttpServletRequest httpRequest) {
        httpRequest.setAttribute("logMessage", "Requesting to assign document with id: %s to project with id: %s".formatted(documentId, projectId));

        return service.assignDocument(projectId, documentId);
    }


    @GetMapping("/projects/{projectId}/documents")
    public List<Document> listDocumentsInProject(@PathVariable Long projectId, HttpServletRequest httpRequest) {
        httpRequest.setAttribute("logMessage", "Requesting to list all documents in project with id: %s".formatted(projectId));

        return service.listDocumentsInProject(projectId);
    }
}
