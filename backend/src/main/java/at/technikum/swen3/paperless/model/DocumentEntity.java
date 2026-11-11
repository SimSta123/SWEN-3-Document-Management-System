package at.technikum.swen3.paperless.model;

import jakarta.persistence.*;
import org.jspecify.annotations.Nullable;

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

    @Column(name = "object_key", length = 512)
    private String objectKey;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    protected DocumentEntity() {}

    public DocumentEntity(String title) {
        this.title = title;
    }

    //Titel = Name des Documents
    //Key = Key in RustFS für das Doku
    public DocumentEntity(String title, String key, @Nullable String originalFilename, String contentType, long size) {
        this.title = title;
        this.objectKey = key;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.sizeBytes = size;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getObjectKey() { return objectKey; }
    public ProjectEntity getProject() { return project; }

    public void assignTo(ProjectEntity project) {
        this.project = project;
    }
}
