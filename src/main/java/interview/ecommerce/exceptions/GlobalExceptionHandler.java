package interview.ecommerce.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);


    @ExceptionHandler(InsufficientStockException.class)
    public final ProblemDetail handleInsufficientStock(InsufficientStockException ex) {
        log.debug("Order rejected, insufficient stock: {}", ex.getMessage(), ex);
        return newProblem(HttpStatus.CONFLICT, "Insufficient stock", ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public final ProblemDetail handleEntityNotFound(ResourceNotFoundException ex) {
        log.debug("Entity not found: {}", ex.getMessage(), ex);
        return newProblem(HttpStatus.NOT_FOUND, "Entity not found", ex.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public final ProblemDetail handleDuplicatedResource(DuplicateResourceException ex) {
        log.debug("Duplicated Resource: {}", ex.getMessage(), ex);
        return newProblem(HttpStatus.CONFLICT, "Duplicated Resource", ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public final ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Constraint violation: {}", ex.getMessage(), ex);
        return newProblem(HttpStatus.CONFLICT, "Duplicated Resource",
                "The resource already exists or violates a database constraint.");
    }

    @ExceptionHandler(InvalidOrderStateException.class)
    public final ProblemDetail handleInvalidOrderState(InvalidOrderStateException ex) {
        log.debug("Invalid order state transition: {}", ex.getMessage(), ex);
        return newProblem(HttpStatus.CONFLICT, "Invalid order state", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public final ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex) {
        log.debug("Illegal Argument Exception: {}", ex.getMessage(), ex);
        return newProblem(HttpStatus.BAD_REQUEST, "Illegal Argument Exception", ex.getMessage());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public final ProblemDetail handleOptimisticLocking(OptimisticLockingFailureException ex) {
        log.debug("Concurrent modification: {}", ex.getMessage(), ex);
        return newProblem(HttpStatus.CONFLICT, "Concurrent modification",
                "The resource was modified by another request.");
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public final ProblemDetail handlePessimisticLocking(PessimisticLockingFailureException ex) {
        log.warn("Lock acquisition failure: {}", ex.getMessage(), ex);
        return newProblem(HttpStatus.CONFLICT, "Concurrent modification",
                "The resource is currently locked by another request, please retry.");
    }

    @ExceptionHandler(Exception.class)
    public final ProblemDetail handleUnexpectedException(Exception ex) {
        log.error("Unexpected Exception: {}", ex.getMessage(), ex);
        return newProblem(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected Exception",
                "An unexpected error occurred while processing the request.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        List<Map<String, String>> errors = new ArrayList<>();

        ex.getBindingResult().getFieldErrors().forEach(fe -> errors.add(Map.of(
                "field", fe.getField(),
                "message", Objects.requireNonNullElse(fe.getDefaultMessage(), "not valid value"))));

        ex.getBindingResult().getGlobalErrors().forEach(ge -> errors.add(Map.of(
                "field", ge.getObjectName(),
                "message", Objects.requireNonNullElse(ge.getDefaultMessage(), "not valid value"))));

        ProblemDetail problem = ex.getBody();
        problem.setTitle("Validation failed");
        problem.setDetail("One or more fields are not valid");
        problem.setProperty("errors", errors);

        log.debug("Validation failed on {}: {}", request.getDescription(false), errors);

        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    private ProblemDetail newProblem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}