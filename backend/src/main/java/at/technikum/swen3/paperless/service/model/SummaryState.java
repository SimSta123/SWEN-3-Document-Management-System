package at.technikum.swen3.paperless.service.model;

import at.technikum.swen3.paperless.model.ProjectSummaryStatus;

import java.util.UUID;

public record SummaryState(UUID requestId, ProjectSummaryStatus status, long readyDocuments, int totalDocuments, String summary) {
    public boolean readyToQueue() {
        return status == ProjectSummaryStatus.WAITING && totalDocuments > 0 && readyDocuments == totalDocuments;
    }
}