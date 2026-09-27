package at.technikum.swen3.paperless.service.mapping;

import at.technikum.swen3.paperless.model.DocumentEntity;
import at.technikum.swen3.paperless.model.ProjectEntity;
import at.technikum.swen3.paperless.service.model.Document;
import at.technikum.swen3.paperless.service.model.Project;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

// MapStruct reference: https://mapstruct.org/documentation/stable/reference/html/
@Mapper(componentModel = "spring")
public interface ProjectMapper {
    Project toProject(ProjectEntity entity);

    @Mapping(target = "projectId", source = "project.id")
    Document toDocument(DocumentEntity entity);
}
