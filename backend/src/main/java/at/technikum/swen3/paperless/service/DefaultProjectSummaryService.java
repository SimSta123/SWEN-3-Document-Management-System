package at.technikum.swen3.paperless.service;

import at.technikum.swen3.paperless.model.DocumentEntity;
import at.technikum.swen3.paperless.model.ProjectEntity;
import at.technikum.swen3.paperless.model.ProjectSummaryStatus;
import at.technikum.swen3.paperless.rabbitMQ.SummaryResultListener;
import at.technikum.swen3.paperless.repository.DocumentRepository;
import at.technikum.swen3.paperless.repository.ProjectRepository;
import at.technikum.swen3.paperless.service.exception.InvalidInputException;
import at.technikum.swen3.paperless.service.exception.NotFoundException;
import at.technikum.swen3.paperless.service.model.SummaryState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import at.technikum.swen3.paperless.rabbitMQ.ProjectSummaryRequest;
import at.technikum.swen3.paperless.rabbitMQ.ProjectSummaryRequestPublisher;

import java.util.List;
import java.util.UUID;

@Service
public class DefaultProjectSummaryService implements ProjectSummaryService {

    private final ProjectRepository projects;
    private final DocumentRepository documents;
    private final ProjectSummaryRequestPublisher publisher;

    public DefaultProjectSummaryService(ProjectRepository projects, DocumentRepository documents, ProjectSummaryRequestPublisher publisher) {
        this.projects = projects;
        this.documents = documents;
        this.publisher = publisher;
    }

    @Override
    @Transactional
    public void startSummary(long projectId) {
        ProjectEntity project = projects.findForUpdate(projectId).orElseThrow(() -> new NotFoundException("Project not found"));

        List<DocumentEntity> pdfs = findProjectPdfs(projectId);
        ProjectSummaryStatus status = project.getSummaryStatus();

        if (status == ProjectSummaryStatus.WAITING || status == ProjectSummaryStatus.QUEUED) {
            toState(project, pdfs);
            return;
        }

        if (pdfs.isEmpty()) {
            throw new InvalidInputException("The project has no PDFs");
        }

        project.startSummaryRequest(UUID.randomUUID());
        toState(project, pdfs);
    }

    @Override
    @Transactional
    public void tryQueue(long projectId) {ProjectEntity project = projects.findForUpdate(projectId).orElse(null);

        if (project == null || project.getSummaryStatus() != ProjectSummaryStatus.WAITING) {
            return;
        }

        List<DocumentEntity> pdfs = findProjectPdfs(projectId);

        if (pdfs.isEmpty()) {
            project.failSummary();
            return;
        }

        SummaryState state = toState(project, pdfs);
        if (!state.readyToQueue()) {
            return;
        }

        List<ProjectSummaryRequest.SummarySource> sources = pdfs.stream()
                .map(pdf -> new ProjectSummaryRequest.SummarySource(pdf.getTitle(), pdf.getSummary()))
                .toList();

        project.markSummaryQueued();
        publisher.publish(new ProjectSummaryRequest(projectId, state.requestId(), sources));
    }

    @Override
    @Transactional(readOnly = true)
    public SummaryState getStatus(long projectId) {
        ProjectEntity project = projects.findById(projectId).orElseThrow(() -> new NotFoundException("Project not found"));

        return toState(project, findProjectPdfs(projectId));
    }

    private List<DocumentEntity> findProjectPdfs(long projectId) {
        return documents.findByProjectIdOrderByIdAsc(projectId).stream()
                .filter(document -> document.getObjectKey() != null).toList();
    }

    private SummaryState toState(ProjectEntity project, List<DocumentEntity> pdfs) {
        long readyDocuments = pdfs.stream().filter(document -> document.getSummary() != null
                        && !document.getSummary().isBlank()).count();

        return new SummaryState(project.getSummaryRequestId(), project.getSummaryStatus(), readyDocuments, pdfs.size(), project.getSummary());
    }

    @Override
    @Transactional
    public void applyResult(SummaryResultListener.ProjectSummaryResult result) {
        ProjectEntity project = projects.findForUpdate(result.projectId()).orElse(null);

        if (project == null
                || project.getSummaryStatus() != ProjectSummaryStatus.QUEUED
                || !result.requestId().equals(project.getSummaryRequestId())) {
            return;
        }

        if (result.summary() != null && !result.summary().isBlank()) {
            project.completeSummary(result.summary().strip());
        } else {
            project.failSummary();
        }
    }
}