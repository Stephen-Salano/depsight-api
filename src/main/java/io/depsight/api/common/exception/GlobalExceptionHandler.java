package io.depsight.api.common.exception;

import io.depsight.api.common.config.ApiResponse;
import io.depsight.api.common.config.ErrorResponse;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
    log.warn("resource not found: {}", ex.getMessage());
    ErrorResponse errorDetails = ErrorResponse.of(ex.getMessage(), "RESOURCE_NOT_FOUND");
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.error(ex.getMessage(), errorDetails));
  }

  // TODO: Add a source field so the error code can be MAVEN_CENTRAL_ERROR or OSS_INDEX_ERROR
  @ExceptionHandler(ExternalApiException.class)
  public ResponseEntity<ApiResponse<Void>> handleExternalApiException(ExternalApiException ex) {
    log.warn("External API exception occurred: {}", ex.getMessage());
    ErrorResponse errorDetails = ErrorResponse.of(ex.getMessage(), "ERROR_WITH_EXTERNAL_API");
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
        .body(ApiResponse.error(ex.getMessage(), errorDetails));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
    Map<String, String> fieldErrors =
        ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));

    ErrorResponse errorDetails =
        ErrorResponse.builder()
            .errorCode("VALIDATION_FAILED")
            .message("Validation failed")
            .timestamp(LocalDateTime.now())
            .validationErrors(fieldErrors)
            .build();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error("Validation failed", errorDetails));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
    log.error("Unexpected error: {}", ex.getMessage(), ex);
    ErrorResponse errorDetails = ErrorResponse.of("An unexpected error occurred", "INTERNAL_ERROR");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.error("An unexpected error occurred", errorDetails));
  }
}
