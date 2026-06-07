package io.depsight.api.common.controller;

import io.depsight.api.common.config.ApiResponse;
import io.depsight.api.common.dto.PageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class BaseController {

  protected <T> ResponseEntity<ApiResponse<T>> ok(T data, String message) {
    return ResponseEntity.ok(ApiResponse.success(data, message));
  }

  protected <T> ResponseEntity<ApiResponse<T>> created(T data, String message) {
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(data, message));
  }

  protected ResponseEntity<ApiResponse<Void>> okMessage(String message) {
    return ResponseEntity.ok(ApiResponse.success(null, message));
  }

  protected <T> ResponseEntity<ApiResponse<PageResponse<T>>> paged(
      PageResponse<T> data, String message) {
    return ResponseEntity.ok(ApiResponse.success(data, message));
  }
}
