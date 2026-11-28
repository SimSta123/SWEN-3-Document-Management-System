package at.technikum.swen3.paperless.service.model;

import at.technikum.swen3.paperless.model.ProjectSummaryStatus;

import java.util.UUID;

public record SummaryState(UUID requestId, ProjectSummaryStatus status, long readyDocuments, int totalDocuments, String summary) {}
