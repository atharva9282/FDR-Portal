package com.aicte.fdrportal.exception;

import com.aicte.fdrportal.service.AuditEventService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final AuditEventService auditEventService;

    public GlobalExceptionHandler(AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            ResourceNotFoundException ex) {

        return build(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                Map.of()
        );
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(
            ConflictException ex) {

        return build(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                Map.of()
        );
    }

//    little different
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors =
                ex.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .collect(Collectors.toMap(
                                error -> error.getField(),
                                error -> error.getDefaultMessage() == null
                                        ? "Invalid value"
                                        : error.getDefaultMessage(),
                                (first, second) -> first,
                                LinkedHashMap::new
                        ));

        return build(
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                errors
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
            ConstraintViolationException ex) {

        Map<String, String> errors = ex.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (first, second) -> first,
                        LinkedHashMap::new
                ));

        return build(
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                errors
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(
            IllegalArgumentException ex) {

        return build(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                Map.of()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {

        return build(
                HttpStatus.BAD_REQUEST,
                "Invalid value for parameter: " + ex.getName(),
                Map.of()
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(
            HttpMessageNotReadableException ex) {

        return build(
                HttpStatus.BAD_REQUEST,
                "Request body is invalid or contains an unsupported value",
                Map.of()
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataConflict(
            DataIntegrityViolationException ex) {

        return build(
                HttpStatus.CONFLICT,
                "The request conflicts with an existing database record",
                Map.of()
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(
            AuthenticationException ex) {

        return build(
                HttpStatus.UNAUTHORIZED,
                "Invalid email or password",
                Map.of()
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
            AccessDeniedException ex) {

        return build(
                HttpStatus.FORBIDDEN,
                ex.getMessage(),
                Map.of()
        );
    }




    private ResponseEntity<ApiError> build(
            HttpStatus status,
            String message,
            Map<String, String> details
    ){
        recordFailedRequest(status, message);

        ApiError body = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                details
        );

        return ResponseEntity.status(status).body(body);
    }

    private void recordFailedRequest(HttpStatus status, String message) {
        try {
            if (RequestContextHolder.getRequestAttributes()
                    instanceof ServletRequestAttributes attributes) {
                HttpServletRequest request = attributes.getRequest();
                String target = request.getRequestURI();
                String details = "method=" + request.getMethod()
                        + "; httpStatus=" + status.value()
                        + "; message=" + (message == null ? "" : message);
                auditEventService.recordFailure(
                        "API_REQUEST_FAILED", "HTTP_REQUEST", target, details);
            }
        } catch (RuntimeException auditFailure) {
            // Auditing must not replace the API error that is being returned.
            log.warn("Unable to persist failed-request audit event", auditFailure);
        }
    }

}
