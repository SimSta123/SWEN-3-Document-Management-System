package at.technikum.swen3.paperless;

import at.technikum.swen3.paperless.controller.ApiExceptionHandler;
import at.technikum.swen3.paperless.controller.DocumentController;
import at.technikum.swen3.paperless.controller.ProjectController;
import at.technikum.swen3.paperless.repository.DocumentRepository;
import at.technikum.swen3.paperless.repository.ProjectRepository;
import at.technikum.swen3.paperless.service.DefaultDocumentService;
import at.technikum.swen3.paperless.service.DefaultProjectService;
import at.technikum.swen3.paperless.service.DocumentService;
import at.technikum.swen3.paperless.service.mapping.ProjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.springframework.dao.*;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.CannotCreateTransactionException;

import java.util.Optional;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import at.technikum.swen3.paperless.rabbitMQ.DocumentEventPublisher;

class ApiExceptionHandlerTest {
    private DocumentRepository documents;
    private MockMvc mvc;

    DocumentEventPublisher publisher = mock(DocumentEventPublisher.class);

    @BeforeEach
    void setUp() {
        documents = mock(DocumentRepository.class);
        Logger logger = mock(Logger.class);
        var service = new DefaultProjectService(mock(ProjectRepository.class), documents, mock(ProjectMapper.class), logger, publisher);
        var docService =  new DefaultDocumentService(documents, mock(ProjectMapper.class), logger, publisher);
        mvc = MockMvcBuilders.standaloneSetup(
                        new ProjectController(service),
                        new DocumentController(docService))
                .setControllerAdvice(new ApiExceptionHandler(logger))
                .build();
    }

    @Test
    void missingDocumentReturns404WithBusinessMessage() throws Exception {
        when(documents.findById(42L)).thenReturn(Optional.empty());
        mvc.perform(get("/documents/42")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Document not found"));
    }

    @Test
    void invalidIdReturns400WithoutCallingTheDatabase() throws Exception {
        mvc.perform(get("/documents/0")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Document id must be positive"));
        verifyNoInteractions(documents);
    }

    @Test
    void invalidRequestBodyReturns400WithoutSaving() throws Exception {
        mvc.perform(post("/documents").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\" \"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value(containsString("title")));
        verifyNoInteractions(documents);
    }

    @Test
    void brokenJsonAndMissingBodyReturn400() throws Exception {
        for (String body : new String[]{"{broken", ""}) {
            mvc.perform(post("/documents").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("The request body is missing or invalid."));
        }
        verifyNoInteractions(documents);
    }

    @Test
    void invalidPathTypeReturns400() throws Exception {
        mvc.perform(get("/documents/not-a-number")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("A request parameter has an invalid value."));
        verifyNoInteractions(documents);
    }

    static Stream<Object[]> failures() {
        return Stream.of(
                new Object[]{new DataAccessResourceFailureException("private database address"), 503},
                new Object[]{new QueryTimeoutException("private SQL"), 503},
                new Object[]{new CannotCreateTransactionException("private connection"), 503},
                new Object[]{new DataIntegrityViolationException("private constraint"), 409},
                new Object[]{new OptimisticLockingFailureException("private row"), 409},
                new Object[]{new InvalidDataAccessResourceUsageException("private SQL"), 500},
                new Object[]{new IllegalStateException("private internal details"), 500});
    }

    @ParameterizedTest
    @MethodSource("failures")
    void repositoryFailuresReturnSafeErrors(RuntimeException failure, int expectedStatus) throws Exception {
        when(documents.findAll(Sort.by("id"))).thenThrow(failure);
        mvc.perform(get("/documents")).andExpect(status().is(expectedStatus))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(content().string(not(containsString("private"))))
                .andExpect(content().string(not(containsString(failure.getClass().getSimpleName()))));
    }

    @Test
    void saveFailureDoesNotReturnCreated() throws Exception {
        when(documents.save(any())).thenThrow(new DataIntegrityViolationException("private constraint"));
        mvc.perform(post("/documents").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Notes\"}"))
                .andExpect(status().isConflict()).andExpect(content().string(not(containsString("private"))));
    }

    @Test
    void unsupportedMethodStillReturns405() throws Exception {
        mvc.perform(delete("/documents/42")).andExpect(status().isMethodNotAllowed());
    }
}
