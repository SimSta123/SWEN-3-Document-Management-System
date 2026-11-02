package at.technikum.swen3.paperless.service;

import at.technikum.swen3.paperless.service.model.Document;
import at.technikum.swen3.paperless.service.model.Project;
import java.util.List;

public interface ProjectService {
    Project createProject(String name, String description);
    List<Project> listProjects();
    Document assignDocument(Long projectId, Long documentId);
    List<Document> listDocumentsInProject(Long projectId);
}
