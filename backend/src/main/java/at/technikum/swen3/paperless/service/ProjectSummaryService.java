package at.technikum.swen3.paperless.service;

import at.technikum.swen3.paperless.rabbitMQ.SummaryResultListener;
import at.technikum.swen3.paperless.service.model.SummaryState;

public interface ProjectSummaryService {
    void startSummary(long projectId);
    void tryQueue(long projectId);
    SummaryState getStatus(long projectId);
    void applyResult(SummaryResultListener.ProjectSummaryResult result);
}