package at.technikum.swen3.paperless.service.model;

import com.fasterxml.jackson.annotation.JsonInclude;

public record Document(Long id, String title, Long projectId, String summary,
                       @JsonInclude(JsonInclude.Include.NON_DEFAULT) boolean hasPdf) {}
