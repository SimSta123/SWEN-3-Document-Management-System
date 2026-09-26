package at.technikum.swen3.paperless.repository;

import at.technikum.swen3.paperless.model.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {}
