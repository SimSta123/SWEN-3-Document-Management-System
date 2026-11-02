package at.technikum.swen3.paperless.service;

import at.technikum.swen3.paperless.service.model.Document;
import java.util.List;

public interface DocumentService {
    Document createDocument(String title);
    List<Document> listAllDocuments();
    Document getDocument(Long documentId);
}
