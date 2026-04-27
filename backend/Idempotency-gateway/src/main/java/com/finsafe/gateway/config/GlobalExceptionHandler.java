package com.finsafe.gateway.config;

import com.finsafe.gateway.exception.IdempotencyConflictException;
import com.finsafe.gateway.exception.InFlightTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Centralised exception → HTTP response mapping.
 *
 * | Exception                       | HTTP status                    |
 * |---------------------------------|--------------------------------|
 * | IdempotencyConflictException    | 409 Conflict                   |
 * | InFlightTimeoutException        | 503 Service Unavailable        |
 * | MethodArgumentNotValidException | 400 Bad Request                |
 * | RuntimeException (catch-all)    | 500 Internal Server Error      |
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── User Story 3: conflicting body → 409 ─────────────────────────────────
    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(IdempotencyConflictException ex) {
        log.warn("Idempotency conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ex.getMessage()));
    }

    // ── Bonus: in-flight timeout → 503 ───────────────────────────────────────
    @ExceptionHandler(InFlightTimeoutException.class)
    public ResponseEntity<ErrorResponse> handleTimeout(InFlightTimeoutException ex) {
        log.warn("In-flight timeout: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse(ex.getMessage()));
    }

    // ── Bean validation failures → 400 ───────────────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("Validation failed: " + details));
    }

    // ── Catch-all → 500 ──────────────────────────────────────────────────────
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException ex) {
        log.error("Unhandled error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("An unexpected error occurred: " + ex.getMessage()));
    }

    // ── Response DTO ─────────────────────────────────────────────────────────
    public record ErrorResponse(String error) {}
}