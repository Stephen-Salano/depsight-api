package io.depsight.api.common.exception;

import io.depsight.api.common.enums.ExternalApiSource;
import lombok.Getter;

/**
 * ExternalApiException for any API errors Source field so error code can be MavenCentral or OSS etc
 */
@Getter
public class ExternalApiException extends RuntimeException {

  private final ExternalApiSource source;
  private final int statusCode;

  public ExternalApiException(String message, ExternalApiSource source) {
    this(message, source, -1);
  }

  public ExternalApiException(String message, ExternalApiSource source, int statusCode) {
    super(message);
    this.source = source;
    this.statusCode = statusCode;
  }
}
