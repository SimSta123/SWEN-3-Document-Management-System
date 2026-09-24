package at.technikum.swen3.paperless.repository;

import at.technikum.swen3.paperless.model.Dbcheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SampleRepository extends JpaRepository<Dbcheck, Long> {

    @Query(value = "SELECT message FROM dbcheck LIMIT 1", nativeQuery = true)
    String getCheck();
}