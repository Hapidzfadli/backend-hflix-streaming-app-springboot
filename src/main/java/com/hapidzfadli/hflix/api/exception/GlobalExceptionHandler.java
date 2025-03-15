package com.hapidzfadli.hflix.api.exception;


import com.hapidzfadli.hflix.api.dto.WebResponseDTO;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<WebResponseDTO<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        WebResponseDTO<Map<String, String>> response = WebResponseDTO.error("Validation failed", errors);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // Handle constraint violations
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<WebResponseDTO<String>> handleConstraintViolation(
            ConstraintViolationException ex) {

        return new ResponseEntity<>(
                WebResponseDTO.error(ex.getMessage()),
                HttpStatus.BAD_REQUEST);
    }

    // Handle entity not found
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<WebResponseDTO<String>> handleEntityNotFound(
            EntityNotFoundException ex) {

        log.error("Entity not found: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(
                WebResponseDTO.error(ex.getMessage()),
                HttpStatus.NOT_FOUND);
    }

    // Handle response status exceptions
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<WebResponseDTO<String>> handleResponseStatusException(
            ResponseStatusException ex) {

        log.error("Response status exception: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(
                WebResponseDTO.error(ex.getReason()),
                ex.getStatusCode());
    }


    // Handle IllegalArgumentException
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<WebResponseDTO<String>> handleIllegalArgumentException(
            IllegalArgumentException ex) {

        log.error("Illegal argument: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(
                WebResponseDTO.error(ex.getMessage()),
                HttpStatus.BAD_REQUEST);
    }

    // Handle general exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<WebResponseDTO<String>> handleAllExceptions(
            Exception ex, WebRequest request) {

        // Log the error
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        return new ResponseEntity<>(
                WebResponseDTO.error("An unexpected error occurred: " + ex.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
