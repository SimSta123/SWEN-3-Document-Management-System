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

import java.util.List;

@Service
@Transactional(readOnly = true)
public class DefaultProjectService implements ProjectService {
    private final ProjectRepository projects;
    private final DocumentRepository documents;
    private final ProjectMapper mapper;

    public DefaultProjectService(ProjectRepository projects, DocumentRepository documents, ProjectMapper mapper) {
        this.projects = projects;
        this.documents = documents;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Project createProject(String name, String description) {
        return mapper.toProject(projects.save(new ProjectEntity(
                name.strip(), description == null ? "" : description.strip())));
    }

    @Override
    public List<Project> listProjects() {
        return projects.findAll(Sort.by("id")).stream().map(mapper::toProject).toList();
    }

    @Override
    @Transactional
    public Document createDocument(String title) {
        return mapper.toDocument(documents.save(new DocumentEntity(title.strip())));
    }

    @Override
    @Transactional
    public Document assignDocument(Long projectId, Long documentId) {
        ProjectEntity project = projects.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        DocumentEntity document = documents.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        // Assigning again is safe; assigning to another project moves the document.
        document.assignTo(project);
        return mapper.toDocument(documents.save(document));
    }

    @Override
    public List<Document> listDocuments(Long projectId) {
        if (!projects.existsById(projectId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }
        return documents.findByProjectIdOrderByIdAsc(projectId).stream().map(mapper::toDocument).toList();
    }
}
