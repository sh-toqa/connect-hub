package org.connecthub.backend.exception;

import org.connecthub.backend.exception.EmailAlreadyExistsException;
import org.connecthub.backend.exception.InvalidCredentialsException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for the ConnectHub backend application.
 * This class uses @RestControllerAdvice to intercept exceptions thrown by controllers and services,
 * and returns consistent HTTP responses with appropriate status codes and error messages.
 *
 * It handles specific exceptions like MethodArgumentNotValidException for validation errors,
 * EmailAlreadyExistsException for registration conflicts, and InvalidCredentialsException for authentication failures.
 * It also includes a catch-all handler for any unhandled exceptions, which logs the error and returns a generic 500 response.
 *
 * Each handler method constructs a response body containing the status code, error type, message, timestamp, and request path,
 * ensuring that clients receive clear and consistent error information.
 */

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 400 Bad Request — validation errors from @Valid in controllers
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        return build(HttpStatus.BAD_REQUEST, "Validation failed", message, request);
    }

    // 409 Conflict — email or username already exists during registration
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(
            EmailAlreadyExistsException ex,
            HttpServletRequest request) {

        return build(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request);
    }

    // 401 Unauthorized — invalid login credentials
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(
            InvalidCredentialsException ex,
            HttpServletRequest request) {

        return build(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage(), request);
    }

    // 500 Internal Server Error — catch-all for any unhandled exceptions, with logging
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                request
        );
    }

    // Helper method to build a consistent error response body with status, error, message, timestamp, and path
    private ResponseEntity<Map<String, Object>> build(
            HttpStatus status, String error, String message, HttpServletRequest request) {

        Map<String, Object> body = Map.of(
                "status",    status.value(),
                "error",     error,
                "message",   message,
                "timestamp", LocalDateTime.now().toString(),
                "path",      request.getRequestURI()
        );
        return ResponseEntity.status(status).body(body);
    }
}
