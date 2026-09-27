package at.technikum.swen3.paperless.repository;

import at.technikum.swen3.paperless.model.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {
    List<DocumentEntity> findByProjectIdOrderByIdAsc(Long projectId);
}
