package at.technikum.swen3.paperless.repository;

import at.technikum.swen3.paperless.model.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {
    List<DocumentEntity> findByProjectIdOrderByIdAsc(Long projectId);

    @Modifying
    @Query("update DocumentEntity d set d.project = null where d.id = :id")
    int removeProject(@Param("id") Long id);

    // The worker takes the same row lock while writing an OCR result to Elasticsearch.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DocumentEntity d where d.id = :id")
    Optional<DocumentEntity> findForDeletion(@Param("id") Long id);
    @Query("select p.id from DocumentEntity d join d.project p where d.id = :documentId")
    Optional<Long> findProjectIdByDocumentId(@Param("documentId") long documentId);
}
