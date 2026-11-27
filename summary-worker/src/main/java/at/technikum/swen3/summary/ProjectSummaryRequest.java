package at.technikum.swen3.summary;

import java.util.List;
import java.util.UUID;

public record ProjectSummaryRequest(long projectId, UUID requestId, List<SummarySource> sources) {
    public record SummarySource(String title, String summary) {}
}