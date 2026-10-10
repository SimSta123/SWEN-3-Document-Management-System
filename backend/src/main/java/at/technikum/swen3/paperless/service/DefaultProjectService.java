package at.technikum.swen3.paperless.service;

import at.technikum.swen3.paperless.model.DocumentEntity;
import at.technikum.swen3.paperless.model.ProjectEntity;
import at.technikum.swen3.paperless.repository.DocumentRepository;
import at.technikum.swen3.paperless.repository.ProjectRepository;
import at.technikum.swen3.paperless.service.mapping.ProjectMapper;
import at.technikum.swen3.paperless.service.model.Document;
import at.technikum.swen3.paperless.service.model.Project;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class DefaultProjectService implements ProjectService {
    private final ProjectRepository projects;
    private final DocumentRepository documents;
    private final ProjectMapper mapper;
    private final Logger logger;

    public DefaultProjectService(ProjectRepository projects, DocumentRepository documents, ProjectMapper mapper, Logger logger) {
        this.projects = projects;
        this.documents = documents;
        this.mapper = mapper;
        this.logger = logger;
    }

    @Override
    @Transactional
    public Project createProject(String name, String description) {
        logger.info("Creating project {}", name);
        return mapper.toProject(projects.save(new ProjectEntity(
                name.strip(), description == null ? "" : description.strip())));
    }

    @Override
    public List<Project> listProjects() {
        logger.info("listing all Projects");
        return projects.findAll(Sort.by("id")).stream().map(mapper::toProject).toList();
    }

    @Override
    @Transactional
    public Document createDocument(String title) {
        logger.info("Creating document {}", title);
        return mapper.toDocument(documents.save(new DocumentEntity(title.strip())));
    }

    @Override
    @Transactional
    public Document assignDocument(Long projectId, Long documentId) {
        logger.info("Assigning document {} to Project {}", documentId,  projectId);
        ProjectEntity project = projects.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        DocumentEntity document = documents.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        // Assigning again is safe; assigning to another project moves the document.
        document.assignTo(project);
        return mapper.toDocument(documents.save(document));
    }

    @Override
    public List<Document> listAllDocuments() {
        logger.info("listing all documents");
        return documents.findAll(Sort.by("id")).stream().map(mapper::toDocument).toList();
    }

    @Override
    public Document getDocument(Long documentId) {
        logger.info("getting document {}", documentId);
        return documents.findById(documentId).map(mapper::toDocument)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
    }

    @Override
    public List<Document> listDocuments(Long projectId) {
        logger.info("listing documents for project {}", projectId);
        if (!projects.existsById(projectId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }
        return documents.findByProjectIdOrderByIdAsc(projectId).stream().map(mapper::toDocument).toList();
    }
}
