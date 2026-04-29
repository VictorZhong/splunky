package com.wpb.spky.common;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException ex, HttpServletRequest request) {
        return body(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), Map.of("path", request.getRequestURI()));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex) {
        return body(HttpStatus.BAD_REQUEST, "MISSING_HEADER",
                "Missing required header: " + ex.getHeaderName(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> details = Map.of("fields", ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList());
        return body(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", details);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
        String reason = ex.getReason() == null ? status.getReasonPhrase() : ex.getReason();
        return body(status, status.name(), reason, Map.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        return body(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception ex) {
        log.error("Unhandled API exception", ex);
        String message = ex.getMessage() == null ? "Unexpected server error" : ex.getMessage();
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", message, Map.of());
    }

    private static ResponseEntity<ErrorResponse> body(HttpStatus status, String code, String message,
                                                      Map<String, Object> details) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(code, message, details, Instant.now()));
    }
}
