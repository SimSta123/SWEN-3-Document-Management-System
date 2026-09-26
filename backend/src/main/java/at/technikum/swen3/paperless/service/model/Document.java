package at.technikum.swen3.paperless.service.model;

// A metadata record only; uploading and storing file contents is a separate use case.
public record Document(Long id, String title, Long projectId) {}
