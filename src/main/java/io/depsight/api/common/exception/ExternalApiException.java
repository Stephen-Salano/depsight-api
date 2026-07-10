package io.depsight.api.common.exception;

import io.depsight.api.common.enums.ExternalApiSource;
import lombok.Getter;

/**
 * ExternalApiException for any API errors Source field so error code can be MavenCentral or OSS etc
 */
@Getter
public class ExternalApiException extends RuntimeException {

  private final ExternalApiSource source;

  public ExternalApiException(String message, ExternalApiSource source) {
    super(message);
    this.source = source;
  }
}
