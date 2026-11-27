package at.technikum.swen3.summary;

import java.util.UUID;

public record ProjectSummaryResult(long projectId, UUID requestId, String summary, String errorMessage) {}