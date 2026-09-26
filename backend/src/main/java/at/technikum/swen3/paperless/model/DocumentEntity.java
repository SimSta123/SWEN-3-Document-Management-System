package at.technikum.swen3.paperless.model;

import jakarta.persistence.*;

@Entity
@Table(name = "documents")
public class DocumentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private ProjectEntity project;

    protected DocumentEntity() {}

    public DocumentEntity(String title) {
        this.title = title;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public ProjectEntity getProject() { return project; }

    public void assignTo(ProjectEntity project) {
        this.project = project;
    }
}
