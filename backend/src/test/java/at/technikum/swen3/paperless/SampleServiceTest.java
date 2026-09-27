package at.technikum.swen3.paperless;

import at.technikum.swen3.paperless.repository.SampleRepository;
import at.technikum.swen3.paperless.service.SampleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SampleServiceTest {

    @Mock
    private SampleRepository repository;

    private SampleService service;

    @BeforeEach
    void setUp() {
        service = new SampleService(repository);
    }

    @Test
    void dbcheckReturnsRepositoryMessage() {
        when(repository.getCheck()).thenReturn("Database connected");

        String result = service.dbcheck();

        assertThat(result).isEqualTo("Database connected");
        verify(repository).getCheck();
    }

    @Test
    void dbcheckPropagatesRepositoryFailure() {
        var failure = new DataAccessResourceFailureException("Database unavailable");
        when(repository.getCheck()).thenThrow(failure);

        assertThatThrownBy(() -> service.dbcheck()).isSameAs(failure);
    }
}
