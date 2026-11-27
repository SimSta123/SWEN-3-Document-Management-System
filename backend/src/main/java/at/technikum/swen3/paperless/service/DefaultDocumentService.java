package at.technikum.swen3.paperless.service;

import at.technikum.swen3.paperless.model.DocumentEntity;
import at.technikum.swen3.paperless.model.ProjectEntity;
import at.technikum.swen3.paperless.rabbitMQ.DocumentEventPublisher;
import at.technikum.swen3.paperless.repository.DocumentRepository;
import at.technikum.swen3.paperless.service.exception.InvalidInputException;
import at.technikum.swen3.paperless.service.exception.NotFoundException;
import at.technikum.swen3.paperless.service.mapping.ProjectMapper;
import at.technikum.swen3.paperless.service.model.Document;
import at.technikum.swen3.paperless.storage.DocumentStorage;
import org.slf4j.Logger;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DefaultDocumentService implements DocumentService {
    private final Logger logger;
    private final DocumentRepository documents;
    private final ProjectMapper mapper;
    private final DocumentEventPublisher eventPublisher;
    private final DocumentStorage storeDocument;
    private final ElasticsearchClient elasticsearchClient;

    public DefaultDocumentService(DocumentRepository documents, ProjectMapper mapper, Logger logger, DocumentEventPublisher eventPublisher, DocumentStorage storeDocument, ElasticsearchClient elasticsearchClient) {
        this.documents = documents;
        this.logger = logger;
        this.eventPublisher = eventPublisher;
        this.mapper = mapper;
        this.storeDocument = storeDocument;
        this.elasticsearchClient = elasticsearchClient;
    }

    @Override
    @Transactional
    public Document createDocument(String title) {
        if (title == null || title.isBlank()) {
            logger.debug("Creating document failed, cause: empty title");
            throw new InvalidInputException("Document title must not be blank");
        }

        String cleanTitle = title.strip();

        if (cleanTitle.length() > 255) {
            logger.debug("Creating document failed, cause: title too long");
            throw new InvalidInputException("Document title must not exceed 255 characters");
        }

        DocumentEntity newDocument = documents.save(new DocumentEntity(cleanTitle));

        eventPublisher.publishDocumentCreated(newDocument.getId());

        logger.info("Created document {} with id: {}", newDocument.getTitle(), newDocument.getId());

        return mapper.toDocument(newDocument);
    }

    @Override
    @Transactional
    public Document createActualDocument(MultipartFile multipartFile, String title) throws IOException {
        if (multipartFile == null) {
            logger.debug("Creating document failed, cause: empty title");
            throw new InvalidInputException("Document title must not be blank");
        }

        String cleanTitle = title == null ? multipartFile.getOriginalFilename() : title;

        if (cleanTitle == null || cleanTitle.isBlank()) {
            throw new InvalidInputException("Document title must not be blank");
        }

        cleanTitle = cleanTitle.strip();

        if (cleanTitle.length() > 255) {
            logger.debug("Creating document failed, cause: title too long");
            throw new InvalidInputException("Document title must not exceed 255 characters");
        }

        String key = storeDocument.upload(multipartFile);

        DocumentEntity newDocument = documents.save(
                new DocumentEntity(cleanTitle, key, multipartFile.getOriginalFilename(), "application/pdf", multipartFile.getSize())
        );

        eventPublisher.publishDocumentCreatedWKey(newDocument.getId(), key);

        logger.info("Created document {} with id: {} with key: {}", newDocument.getTitle(), newDocument.getId(), key);

        return mapper.toDocument(newDocument);
    }

    @Override
    public List<Document> listAllDocuments() {
        List<Document> documentList = documents.findAll(Sort.by("id")).stream().map(mapper::toDocument).toList();
        logger.debug("Listed documents: count={}", documentList.size());
        return documentList;
    }

    @Override
    public Document getDocument(Long documentId) {
        if (documentId == null || documentId <= 0) {
            logger.debug("Getting document failed, cause: document id must be positive, got id: {}", documentId);
            throw new InvalidInputException("Document id must be positive");
        }

        Document documentGet = documents.findById(documentId).map(mapper::toDocument)
                .orElseThrow(() -> {
                    logger.debug("Getting document failed, cause: document id not found, got id: {}", documentId);
                    return new NotFoundException("Document not found");
                });

        logger.debug("Fetched documentId={}", documentId);
        return documentGet;
    }

    @Override
    @Transactional(rollbackFor = IOException.class)
    public void deleteDocument(Long documentId) throws IOException{
        if (documentId == null || documentId <= 0) {
            throw new InvalidInputException("Document id must be positive");
        }
        DocumentEntity document = documents.findForDeletion(documentId)
                .orElseThrow(() -> new NotFoundException("Document not found"));

        String key = document.getObjectKey();

        if (key != null) {
            storeDocument.delete(key);
        }

        elasticsearchClient.delete(documentId);

        documents.delete(document);
    }

    @Override
    public List<Document> searchDocuments(String term) throws IOException {
        if (term == null || term.isBlank()) {
            return listAllDocuments();
        }

        List<Long> matchingIds = elasticsearchClient.search(term);

        if (matchingIds.isEmpty()) {
            return List.of();
        }

        var documentsById = documents.findAllById(matchingIds)
                .stream()
                .collect(Collectors.toMap(
                        DocumentEntity::getId,
                        mapper::toDocument
                ));

        return matchingIds.stream()
                .map(documentsById::get)
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional
    @Override
    public void saveSummary(long documentId, String summary) {
        if (documentId <= 0) {
            throw new InvalidInputException("Document id must be positive");
        }
        if (summary == null || summary.isBlank()) {
            throw new InvalidInputException("Document summary must not be blank");
        }

        String normalized = summary.strip();

        /*
        //Wenn schon eine Summary da ist, wirf eine Exception
        //Es kann noch eine kommen wenn zb bug oder rabbit Abgestürt
        documents.findForDeletion(documentId).ifPresent(document -> {
            if (document.getSummary() != null) {
                throw new IllegalStateException(
                        "Document " + documentId + " already has a summary");
            }

            document.setSummary(normalized);
        });
         */

// Bei erneuter Zustellung bleibt die bereits gespeicherte Summary erhalten.
        documents.findForDeletion(documentId).filter(document -> document.getSummary() == null
                        || document.getSummary().isBlank())
                .ifPresent(document -> document.setSummary(normalized));
    }
}
