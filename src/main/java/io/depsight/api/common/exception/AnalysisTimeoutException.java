package io.depsight.api.common.exception;

import io.depsight.api.common.enums.ExternalApiSource;

public class AnalysisTimeoutException extends ExternalApiException {
  public AnalysisTimeoutException(String message, ExternalApiSource source) {
    super(message, source);
  }
}
