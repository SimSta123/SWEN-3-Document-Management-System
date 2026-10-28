package at.technikum.swen3.paperless.controller;

import at.technikum.swen3.paperless.service.exception.InvalidInputException;
import at.technikum.swen3.paperless.service.exception.NotFoundException;
import org.slf4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.beans.TypeMismatchException;
import org.springframework.transaction.CannotCreateTransactionException;

// Keep HTTP errors here, so the service does not need HTTP classes.
// https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-advice.html
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    private final Logger logger;

    public ApiExceptionHandler(Logger logger) {
        this.logger = logger;
    }

    @ExceptionHandler(InvalidInputException.class)
    public ProblemDetail invalidInput(InvalidInputException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail notFound(NotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    // Spring Data already translates database errors into these exceptions.
    // https://docs.spring.io/spring-framework/reference/data-access/dao.html
    @ExceptionHandler(DataAccessException.class)
    public ProblemDetail databaseFailure(DataAccessException exception) {
        logger.error("Database request failed", exception);
        if (exception instanceof DataIntegrityViolationException
                || exception instanceof OptimisticLockingFailureException) {
            return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "The data could not be saved. Reload and try again.");
        }
        if (exception instanceof DataAccessResourceFailureException
                || exception instanceof TransientDataAccessException) {
            return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "The database is unavailable. Try again later.");
        }
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "The database request failed.");
    }

    @ExceptionHandler(CannotCreateTransactionException.class)
    public ProblemDetail transactionUnavailable(CannotCreateTransactionException exception) {
        logger.error("Could not start database transaction", exception);
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "The database is unavailable. Try again later.");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail unexpectedFailure(Exception exception) {
        logger.error("Request failed", exception);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Try again later.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .sorted().distinct().collect(java.util.stream.Collectors.joining(", "));
        return handleExceptionInternal(exception, ProblemDetail.forStatusAndDetail(status, detail), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(exception, ProblemDetail.forStatusAndDetail(status, "The request body is missing or invalid."),
                headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(exception, ProblemDetail.forStatusAndDetail(status, "A request parameter has an invalid value."),
                headers, status, request);
    }
}
