package io.depsight.api.common.config;

import java.time.LocalDateTime;

/**
 * A standard generic API response Wrapper
 *
 * @param success Inidcates if the request was successful
 * @param message A descriptive message about the Outcome
 * @param data The Payload of the response
 * @param errorResponse detailed error information, if any
 * @param timestamp The time the response was generated
 * @param <T> The type of the data payload
 */
public record ApiResponse<T>(
    boolean success, String message, T data, ErrorResponse errorResponse, LocalDateTime timestamp) {
  /**
   * @param <T> The type of the Payload
   * @param data the response payload
   * @param message A success message.
   * @return a success ApirResponse instance.
   */
  public static <T> ApiResponse<T> success(T data, String message) {
    return new ApiResponse<>(true, message, data, null, LocalDateTime.now());
  }

  /**
   * A simple error response.
   *
   * @param <T> the type of the payload (will be null.)
   * @param message the error message.
   * @return A simple error ApiResponse instance.
   */
  public static <T> ApiResponse<T> error(String message) {
    return new ApiResponse<T>(false, message, null, null, LocalDateTime.now());
  }

  /**
   * @param <T> The type of payload (will be null)
   * @param message the error message.
   * @param errorDetails detailed error information.
   * @return a detailed error ApiResponse instance.
   */
  public static <T> ApiResponse<T> error(String message, ErrorResponse errorDetails) {
    return new ApiResponse<T>(false, message, null, errorDetails, LocalDateTime.now());
  }
}
