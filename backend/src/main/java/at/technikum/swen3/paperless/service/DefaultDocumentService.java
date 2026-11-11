package at.technikum.swen3.paperless.service;

import at.technikum.swen3.paperless.model.DocumentEntity;
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

@Service
@Transactional(readOnly = true)
public class DefaultDocumentService implements DocumentService {
    private final Logger logger;
    private final DocumentRepository documents;
    private final ProjectMapper mapper;
    private final DocumentEventPublisher eventPublisher;
    private final DocumentStorage storeDocument;


    public DefaultDocumentService(DocumentRepository documents, ProjectMapper mapper, Logger logger, DocumentEventPublisher eventPublisher, DocumentStorage storeDocument) {
        this.documents = documents;
        this.logger = logger;
        this.eventPublisher = eventPublisher;
        this.mapper = mapper;
        this.storeDocument = storeDocument;
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
}
