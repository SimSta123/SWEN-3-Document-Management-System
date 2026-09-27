package at.technikum.swen3.paperless.model;

import jakarta.persistence.*;

// JPA entity reference: https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2
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

    protected ProjectEntity() {}

    public ProjectEntity(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
}
