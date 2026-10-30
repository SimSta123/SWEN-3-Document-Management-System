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
import jakarta.servlet.http.HttpServletRequest;

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

    //May be deletet
    public record CreateDocumentRequest(@NotBlank @Size(max = 255) String title) {}

    @PostMapping("/projects")
    @ResponseStatus(HttpStatus.CREATED)
    public Project createProject(@Valid @RequestBody CreateProjectRequest request, HttpServletRequest httpRequest) {
        /*
        logger.info("Request: method={} path={} remoteAddress={}, requesting to create project withn name: {}, description: {}",
                httpRequest.getMethod(),
                httpRequest.getRequestURI(),
                httpRequest.getRemoteAddr(),
                request.name(),
                request.description());
         */
        httpRequest.setAttribute("logMessage", "Requesting to create project with name: %s, description: %s".formatted(request.name(), request.description()));

        return service.createProject(request.name(), request.description());
    }

    @GetMapping("/projects")
    public List<Project> listProjects(HttpServletRequest httpRequest) {
        /*
        logger.info("Request: method={} path={} remoteAddress={}, requesting a list of all projects",
                httpRequest.getMethod(),
                httpRequest.getRequestURI(),
                httpRequest.getRemoteAddr());
         */
        httpRequest.setAttribute("logMessage", "Requesting to list all projects");

        return service.listProjects();
    }

    //May be deletet
    /*
    @PostMapping("/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public Document createDocument(@Valid @RequestBody CreateDocumentRequest request, HttpServletRequest httpRequest) {
        logger.info("Request: method={} path={} remoteAddress={}, requesting to create document with title: {}",
                httpRequest.getMethod(),
                httpRequest.getRequestURI(),
                httpRequest.getRemoteAddr(),
                request.title());
        return service.createDocument(request.title());
    }
     */

    @PutMapping("/projects/{projectId}/documents/{documentId}")
    public Document assignDocument(@PathVariable Long projectId, @PathVariable Long documentId, HttpServletRequest httpRequest) {
        /*
        logger.info("Request: method={} path={} remoteAddress={}, requesting to assign document with id:{} to project with id: {}",
                httpRequest.getMethod(),
                httpRequest.getRequestURI(),
                httpRequest.getRemoteAddr(),
                documentId,
                projectId);
         */
        httpRequest.setAttribute("logMessage", "Requesting to requesting to assign document with id: %s to project with id: %s".formatted(documentId, projectId));

        return service.assignDocument(projectId, documentId);
    }

    //May be Deletet
    /*
    @GetMapping("/documents")
    public List<Document> listAllDocuments(HttpServletRequest httpRequest) {
        logger.info("Request: method={} path={} remoteAddress={}, requesting a list of all documents",
                httpRequest.getMethod(),
                httpRequest.getRequestURI(),
                httpRequest.getRemoteAddr());
        return service.listAllDocuments();
    }
     */

    //May be deleted
    /*
    @GetMapping("/documents/{documentId}")
    public Document getDocument(@PathVariable Long documentId, HttpServletRequest httpRequest) {
        logger.info("Request: method={} path={} remoteAddress={}, requesting to get document with id: {}",
                httpRequest.getMethod(),
                httpRequest.getRequestURI(),
                httpRequest.getRemoteAddr(),
                documentId);
        return service.getDocument(documentId);
    }
     */

    @GetMapping("/projects/{projectId}/documents")
    public List<Document> listDocumentsInProject(@PathVariable Long projectId, HttpServletRequest httpRequest) {
        /*
        logger.info("Request: method={} path={} remoteAddress={}, requesting to get all documents of project with id: {}",
                httpRequest.getMethod(),
                httpRequest.getRequestURI(),
                httpRequest.getRemoteAddr(),
                projectId);
         */
        httpRequest.setAttribute("logMessage", "Requesting to list all documents in project with id: %s".formatted(projectId));

        return service.listDocumentsInProject(projectId);
    }
}
