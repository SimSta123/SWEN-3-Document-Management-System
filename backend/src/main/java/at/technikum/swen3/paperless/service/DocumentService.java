package at.technikum.swen3.paperless.service;

import at.technikum.swen3.paperless.service.model.Document;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface DocumentService {
    Document createDocument(String title);
    Document createActualDocument(MultipartFile multipartFile, String title) throws IOException;
    List<Document> listAllDocuments();
    Document getDocument(Long documentId);
    void deleteDocument(Long documentId) throws IOException;
    List<Document> searchDocuments(String term) throws IOException;
    void saveSummary(long documentId, String summary);
    void markSummaryFailed(long documentId);
}
