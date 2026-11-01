package at.technikum.swen3.paperless;

import at.technikum.swen3.paperless.model.DocumentEntity;
import at.technikum.swen3.paperless.repository.DocumentRepository;
import at.technikum.swen3.paperless.service.DefaultDocumentService;
import at.technikum.swen3.paperless.service.mapping.ProjectMapper;
import at.technikum.swen3.paperless.service.model.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import at.technikum.swen3.paperless.service.exception.InvalidInputException;
import org.slf4j.Logger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import at.technikum.swen3.paperless.rabbitMQ.DocumentEventPublisher;

@ExtendWith(MockitoExtension.class)
class DefaultDocumentServiceTest {

    @Mock
    private DocumentRepository documents;

    @Mock
    private ProjectMapper mapper;

    private DefaultDocumentService service;

    @Mock
    private Logger logger;

    @Mock
    DocumentEventPublisher publisher;

    @BeforeEach
    void setUp() {
        service = new DefaultDocumentService(documents, mapper, logger, publisher);
    }
    @Test
    void createDocumentTrimsTitle() {
        DocumentEntity saved = new DocumentEntity("Invoice");
        Document expected = new Document(10L, "Invoice", null);

        when(documents.save(any(DocumentEntity.class))).thenReturn(saved);
        when(mapper.toDocument(saved)).thenReturn(expected);

        Document result = service.createDocument("  Invoice  ");

        ArgumentCaptor<DocumentEntity> captor = ArgumentCaptor.forClass(DocumentEntity.class);
        verify(documents).save(captor.capture());

        assertThat(captor.getValue().getTitle()).isEqualTo("Invoice");
        assertThat(captor.getValue().getProject()).isNull();
        assertThat(result).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    //Testen null, "", "  " und umbruch in ""
    void createDocumentRejectsBlankTitle(String title) {
        InvalidInputException exception = assertThrows(InvalidInputException.class,
                () -> service.createDocument(title)
        );

        assertThat(exception.getMessage()).isEqualTo("Document title must not be blank");

        verifyNoInteractions(documents, mapper);
    }

    @Test
    void createDocumentRejectsTitleLongerThan255Characters() {
        InvalidInputException exception = assertThrows(InvalidInputException.class,
                () -> service.createDocument("a".repeat(256))
        );

        assertThat(exception.getMessage()).isEqualTo("Document title must not exceed 255 characters");

        verifyNoInteractions(documents, mapper);
    }

    @Test
    void createDocumentAcceptsTitleWith255Characters() {
        DocumentEntity saved = new DocumentEntity("a".repeat(255));
        Document expected = new Document(10L, "a".repeat(255), null);

        when(documents.save(any(DocumentEntity.class))).thenReturn(saved);
        when(mapper.toDocument(saved)).thenReturn(expected);

        Document result = service.createDocument("a".repeat(255));

        ArgumentCaptor<DocumentEntity> captor = ArgumentCaptor.forClass(DocumentEntity.class);
        verify(documents).save(captor.capture());

        assertThat(captor.getValue().getTitle()).isEqualTo("a".repeat(255));
        assertThat(captor.getValue().getProject()).isNull();
        assertThat(result).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource(value = {"null", "-1", "0"}, nullValues = "null")
    void GetDocumentRejectsInvalidIds(Long documentId) {
        InvalidInputException exception = assertThrows(InvalidInputException.class,
                () -> service.getDocument(documentId)
        );

        assertThat(exception.getMessage()).isEqualTo("Document id must be positive");

        verifyNoInteractions(documents, mapper);
    }
}
