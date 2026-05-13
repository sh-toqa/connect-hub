package org.connecthub.backend.exception;

import org.connecthub.backend.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
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
 * Central exception handler. Every unhandled exception ends up here
 * and is returned as a structured JSON ApiErrorResponse — never a raw stack trace.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Validation failures (400) ─────────────────────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {

        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        (a, b) -> a   // keep first message if field appears twice
                ));

        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                LocalDateTime.now(), 400, "Validation Failed",
                "One or more fields are invalid", req.getRequestURI(), fieldErrors));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(
            IllegalArgumentException ex,
            HttpServletRequest req) {

        return build(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                ex.getMessage(),
                req
        );
    }

    // ── Business rule violations (409) ───────────────────────────────────────
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailExists(
            EmailAlreadyExistsException ex, HttpServletRequest req) {
        return conflict(ex.getMessage(), req);
    }

    @ExceptionHandler(FriendshipConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleFriendshipConflict(
            FriendshipConflictException ex,
            HttpServletRequest req) {

        return conflict(ex.getMessage(), req);
    }

    // ── Authentication failures (401) ────────────────────────────────────────
    @ExceptionHandler({InvalidCredentialsException.class, InvalidPasswordException.class})
    public ResponseEntity<ApiErrorResponse> handleAuth(
            RuntimeException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage(), req);
    }

    // ── Not found (404) ──────────────────────────────────────────────────────
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), req);
    }

    // ── File storage failures (500) ───────────────────────────────────────────
    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ApiErrorResponse> handleFileStorage(
            FileStorageException ex, HttpServletRequest req) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "File Storage Error", ex.getMessage(), req);
    }

    // ── Catch-all (500) ───────────────────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleAll(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred", req);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private ResponseEntity<ApiErrorResponse> conflict(String message, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "Conflict", message, req);
    }

    private ResponseEntity<ApiErrorResponse> build(
            HttpStatus status, String error, String message, HttpServletRequest req) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                LocalDateTime.now(), status.value(), error, message, req.getRequestURI(), null));
    }
}