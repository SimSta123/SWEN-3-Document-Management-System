package at.technikum.swen3.paperless;

import at.technikum.swen3.paperless.model.DocumentEntity;
import at.technikum.swen3.paperless.model.ProjectEntity;
import at.technikum.swen3.paperless.repository.DocumentRepository;
import at.technikum.swen3.paperless.repository.ProjectRepository;
import at.technikum.swen3.paperless.service.DefaultProjectService;
import at.technikum.swen3.paperless.service.mapping.ProjectMapper;
import at.technikum.swen3.paperless.service.model.Document;
import at.technikum.swen3.paperless.service.model.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultProjectServiceTest {

    @Mock
    private ProjectRepository projects;

    @Mock
    private DocumentRepository documents;

    @Mock
    private ProjectMapper mapper;

    private DefaultProjectService service;

    @BeforeEach
    void setUp() {
        service = new DefaultProjectService(projects, documents, mapper);
    }

    @Test
    void createProjectTrimsNameAndDescription() {
        ProjectEntity saved = new ProjectEntity("University", "Course documents");
        Project expected = new Project(1L, "University", "Course documents");

        when(projects.save(any(ProjectEntity.class))).thenReturn(saved);
        when(mapper.toProject(saved)).thenReturn(expected);

        Project result = service.createProject("  University  ", "  Course documents  ");

        ArgumentCaptor<ProjectEntity> captor = ArgumentCaptor.forClass(ProjectEntity.class);
        verify(projects).save(captor.capture());

        assertThat(captor.getValue().getName()).isEqualTo("University");
        assertThat(captor.getValue().getDescription()).isEqualTo("Course documents");
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void createProjectConvertsNullDescriptionToEmptyString() {
        ProjectEntity saved = new ProjectEntity("University", "");
        Project expected = new Project(1L, "University", "");

        when(projects.save(any(ProjectEntity.class))).thenReturn(saved);
        when(mapper.toProject(saved)).thenReturn(expected);

        Project result = service.createProject("University", null);

        ArgumentCaptor<ProjectEntity> captor = ArgumentCaptor.forClass(ProjectEntity.class);
        verify(projects).save(captor.capture());

        assertThat(captor.getValue().getDescription()).isEmpty();
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void listProjectsRequestsIdSortingAndMapsResults() {
        ProjectEntity first = new ProjectEntity("First", "");
        ProjectEntity second = new ProjectEntity("Second", "");
        Project firstResult = new Project(1L, "First", "");
        Project secondResult = new Project(2L, "Second", "");

        when(projects.findAll(Sort.by("id"))).thenReturn(List.of(first, second));
        when(mapper.toProject(first)).thenReturn(firstResult);
        when(mapper.toProject(second)).thenReturn(secondResult);

        assertThat(service.listProjects()).containsExactly(firstResult, secondResult);

        verify(projects).findAll(Sort.by("id"));
    }

    @Test
    void listProjectsReturnsEmptyListWhenNoProjectsExist() {
        when(projects.findAll(Sort.by("id"))).thenReturn(List.of());

        assertThat(service.listProjects()).isEmpty();

        verifyNoInteractions(mapper);
    }

    @Test
    void createDocumentTrimsTitle() {
        DocumentEntity saved = new DocumentEntity("Invoice");
        Document expected = new Document(10L, "Invoice", null);

        when(documents.save(any(DocumentEntity.class))).thenReturn(saved);
        when(mapper.toDocument(saved)).thenReturn(expected);

        Document result = service.createDocument("  Invoice  ");

        ArgumentCaptor<DocumentEntity> captor = ArgumentCaptor.forClass(DocumentEntity.class);
        verify(documents).save(captor.capture());

        assertThat(captor.getValue().getTitle()).isEqualTo("Invoice");
        assertThat(captor.getValue().getProject()).isNull();
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void assignDocumentAssignsAndSavesDocument() {
        ProjectEntity project = new ProjectEntity("University", "");
        DocumentEntity document = new DocumentEntity("Notes");
        Document expected = new Document(10L, "Notes", 1L);

        when(projects.findById(1L)).thenReturn(Optional.of(project));
        when(documents.findById(10L)).thenReturn(Optional.of(document));
        when(documents.save(document)).thenReturn(document);
        when(mapper.toDocument(document)).thenReturn(expected);

        Document result = service.assignDocument(1L, 10L);

        assertThat(document.getProject()).isSameAs(project);
        assertThat(result).isEqualTo(expected);
        verify(documents).save(document);
    }

    @Test
    void assignDocumentMovesDocumentToAnotherProject() {
        ProjectEntity oldProject = new ProjectEntity("Old", "");
        ProjectEntity newProject = new ProjectEntity("New", "");
        DocumentEntity document = new DocumentEntity("Notes");
        document.assignTo(oldProject);
        Document expected = new Document(10L, "Notes", 2L);

        when(projects.findById(2L)).thenReturn(Optional.of(newProject));
        when(documents.findById(10L)).thenReturn(Optional.of(document));
        when(documents.save(document)).thenReturn(document);
        when(mapper.toDocument(document)).thenReturn(expected);

        Document result = service.assignDocument(2L, 10L);

        assertThat(document.getProject()).isSameAs(newProject);
        assertThat(result).isEqualTo(expected);
        verify(documents).save(document);
    }

    @Test
    void assignDocumentToSameProjectAgainIsSafe() {
        ProjectEntity project = new ProjectEntity("University", "");
        DocumentEntity document = new DocumentEntity("Notes");
        document.assignTo(project);
        Document expected = new Document(10L, "Notes", 1L);

        when(projects.findById(1L)).thenReturn(Optional.of(project));
        when(documents.findById(10L)).thenReturn(Optional.of(document));
        when(documents.save(document)).thenReturn(document);
        when(mapper.toDocument(document)).thenReturn(expected);

        assertThat(service.assignDocument(1L, 10L)).isEqualTo(expected);
        assertThat(document.getProject()).isSameAs(project);
        verify(documents).save(document);
    }

    @Test
    void assignDocumentThrowsNotFoundWhenProjectDoesNotExist() {
        when(projects.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException exception = catchThrowableOfType(
                ResponseStatusException.class,
                () -> service.assignDocument(1L, 10L));

        assertThat(exception).isNotNull();
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getReason()).isEqualTo("Project not found");
        verifyNoInteractions(documents, mapper);
    }

    @Test
    void assignDocumentThrowsNotFoundWhenDocumentDoesNotExist() {
        ProjectEntity project = new ProjectEntity("University", "");

        when(projects.findById(1L)).thenReturn(Optional.of(project));
        when(documents.findById(10L)).thenReturn(Optional.empty());

        ResponseStatusException exception = catchThrowableOfType(
                ResponseStatusException.class,
                () -> service.assignDocument(1L, 10L));

        assertThat(exception).isNotNull();
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getReason()).isEqualTo("Document not found");
        verify(documents, never()).save(any(DocumentEntity.class));
        verifyNoInteractions(mapper);
    }

    @Test
    void listDocumentsReturnsMappedDocumentsInRepositoryOrder() {
        DocumentEntity first = new DocumentEntity("First");
        DocumentEntity second = new DocumentEntity("Second");
        Document firstResult = new Document(10L, "First", 1L);
        Document secondResult = new Document(11L, "Second", 1L);

        when(projects.existsById(1L)).thenReturn(true);
        when(documents.findByProjectIdOrderByIdAsc(1L)).thenReturn(List.of(first, second));
        when(mapper.toDocument(first)).thenReturn(firstResult);
        when(mapper.toDocument(second)).thenReturn(secondResult);

        assertThat(service.listDocuments(1L)).containsExactly(firstResult, secondResult);

        verify(documents).findByProjectIdOrderByIdAsc(1L);
    }

    @Test
    void listDocumentsReturnsEmptyListWhenProjectHasNoDocuments() {
        when(projects.existsById(1L)).thenReturn(true);
        when(documents.findByProjectIdOrderByIdAsc(1L)).thenReturn(List.of());

        assertThat(service.listDocuments(1L)).isEmpty();

        verifyNoInteractions(mapper);
    }

    @Test
    void listDocumentsThrowsNotFoundWhenProjectDoesNotExist() {
        when(projects.existsById(1L)).thenReturn(false);

        ResponseStatusException exception = catchThrowableOfType(
                ResponseStatusException.class,
                () -> service.listDocuments(1L));

        assertThat(exception).isNotNull();
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getReason()).isEqualTo("Project not found");
        verifyNoInteractions(documents, mapper);
    }
}