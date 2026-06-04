package io.depsight.api.common.config;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

/**
 * We use class here instead of java records becuase records are immutable by nature, we need
 * flexibility which is brought in by the @getters and @setters
 */
public class ErrorResponse {
  /**
   * @param message the error message.
   * @param errorCode the http error code
   * @return a new ErrorResponse instance.
   */
  public static ErrorResponse of(String message, String errorCode) {
    return ErrorResponse.builder()
        .message(message)
        .errorCode(errorCode)
        .timestamp(LocalDateTime.now())
        .build();
  }

  private String message;
  private String errorCode;
  private LocalDateTime timestamp;

  private Map<String, String> validationErrors;
}
