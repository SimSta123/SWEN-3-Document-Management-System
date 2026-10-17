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

import javax.print.Doc;
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
        if (name == null || name.isBlank()) {
            logger.debug("Creating project failed, cause: empty name");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project name must not be blank");
        }

        String cleanName = name.strip();

        if (cleanName.length() > 200) {
            logger.debug("Creating project failed, cause: name too long");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project name must not exceed 200 characters");
        }

        String cleanDescription = description == null ? "" : description.strip();

        if (cleanDescription.length() > 2000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project description must not exceed 2000 characters");
        }

        ProjectEntity newProject = projects.save(new ProjectEntity(cleanName, cleanDescription));

        logger.info("Created project :{}, description: {}", newProject.getName(), newProject.getDescription());

        return mapper.toProject(newProject);
    }

    @Override
    public List<Project> listProjects() {
        List<Project> projectList =  projects.findAll(Sort.by("id")).stream().map(mapper::toProject).toList();
        logger.debug("Listed projects: count={}", projectList.size());
        return projectList;
    }

    @Override
    @Transactional
    public Document createDocument(String title) {
        if (title == null || title.isBlank()) {
            logger.debug("Creating document failed, cause: empty title");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document title must not be blank");
        }

        String cleanTitle = title.strip();

        if (cleanTitle.length() > 255) {
            logger.debug("Creating document failed, cause: title too long");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document title must not exceed 255 characters");
        }

        DocumentEntity newDocument = documents.save(new DocumentEntity(cleanTitle));

        logger.info("Created document {}", newDocument.getTitle());

        return mapper.toDocument(newDocument);
    }

    @Override
    @Transactional
    public Document assignDocument(Long projectId, Long documentId) {
        if (projectId == null || projectId <= 0 || documentId == null || documentId <= 0) {
            logger.debug("Assigning document failed, cause: project or document id must not be null");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IDs must be positive");
        }

        ProjectEntity project = projects.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        DocumentEntity document = documents.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        // Assigning again is safe; assigning to another project moves the document.
        document.assignTo(project);

        DocumentEntity newDocument = documents.save(document);

        logger.info("Assigned documentId={} to projectId={}", newDocument.getId(), projectId);

        return mapper.toDocument(newDocument);

    }

    @Override
    public List<Document> listAllDocuments() {
        List<Document> documentList = documents.findAll(Sort.by("id")).stream().map(mapper::toDocument).toList();
        logger.debug("Listed documents: count={}", documentList.size());
        return documentList;
    }

    @Override
    public Document getDocument(Long documentId) {
        if (documentId == null || documentId <= 0) {
            logger.debug("Getting document failed, cause: document id must not be null");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document ID must be positive");
        }

        Document documentGet = documents.findById(documentId).map(mapper::toDocument)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        logger.debug("Fetched documentId={}", documentId);
        return documentGet;
    }

    @Override
    public List<Document> listDocumentsInProject(Long projectId) {
        if (projectId == null || projectId <= 0) {
            logger.debug("listing documents failed, cause: project id must not be null");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project ID must be positive");
        }

        if (!projects.existsById(projectId)) {
            logger.debug("listing documents failed, cause: project not found");
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }

        List<Document> documentsList = documents.findByProjectIdOrderByIdAsc(projectId).stream().map(mapper::toDocument).toList();

        logger.debug("Listed documents: count={} for project {}", documentsList.size(), projectId);

        return documentsList;
    }
}
