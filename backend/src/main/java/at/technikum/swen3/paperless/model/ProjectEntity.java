package at.technikum.swen3.paperless.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "projects")
public class ProjectEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "summary_status", nullable = false, length = 20)
    private ProjectSummaryStatus summaryStatus = ProjectSummaryStatus.IDLE;

    @Column(name = "summary_request_id")
    private UUID summaryRequestId;

    protected ProjectEntity() {}

    public ProjectEntity(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }

    public String getSummary() {
        return summary;
    }

    public ProjectSummaryStatus getSummaryStatus() {
        return summaryStatus;
    }

    public UUID getSummaryRequestId() {
        return summaryRequestId;
    }

    public void startSummaryRequest(UUID requestId) {
        this.summaryRequestId = requestId;
        this.summaryStatus = ProjectSummaryStatus.WAITING;
    }

    public void markSummaryQueued() {
        this.summaryStatus = ProjectSummaryStatus.QUEUED;
    }

    public void completeSummary(String summary) {
        this.summary = summary;
        this.summaryStatus = ProjectSummaryStatus.COMPLETED;
    }

    public void failSummary() {
        this.summaryStatus = ProjectSummaryStatus.FAILED;
    }
}
