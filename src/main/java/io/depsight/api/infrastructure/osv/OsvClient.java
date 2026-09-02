package io.depsight.api.infrastructure.osv;

import io.depsight.api.common.enums.ExternalApiSource;
import io.depsight.api.common.exception.ExternalApiException;
import io.depsight.api.infrastructure.osv.dto.request.BatchRequest;
import io.depsight.api.infrastructure.osv.dto.response.BatchResponse;
import io.depsight.api.infrastructure.osv.dto.response.OsvVulnerability;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
@Slf4j
public class OsvClient {

  private final WebClient client;
  private static final String QUERYBATCH_URL = "/v1/querybatch";
  private static final String VULNERABILITY_URL = "/v1/vulns/{id}";
  private static final String BATCH_MESSAGE = "Failed to query OSV batch vulnerability endpoint";
  private static final String VULN_MESSAGE = "Failed to query single vulnerability endpoint";

  public OsvClient(WebClient.Builder builder) {
    this.client = builder.baseUrl("https://api.osv.dev").build();
  }

  public Mono<BatchResponse> queryBatch(BatchRequest request) {

    return Mono.defer(() -> client
            .post() // build the request
            .uri(QUERYBATCH_URL) // where to
            .bodyValue(request) // put the request inside
            .retrieve() // fire off the request
            .onStatus(
                status -> status.isError(),
                resp -> error(BATCH_MESSAGE, ExternalApiSource.OSV_DEV, resp.statusCode()))
            .bodyToMono(BatchResponse.class)) // what comes back
        .retryWhen(retryBackoff());
  }

  public Mono<OsvVulnerability> fetchVulnerability(String id) {

    return Mono.defer(() -> client
            .get()
            .uri(VULNERABILITY_URL, id) // webclient will replace the id in the endpoint
            .retrieve()
            .onStatus(
                status -> status.isError(),
                resp -> error(VULN_MESSAGE, ExternalApiSource.OSV_DEV, resp.statusCode()))
            .bodyToMono(OsvVulnerability.class))
        .retryWhen(retryBackoff());
  }

  private Retry retryBackoff() {
    return Retry.backoff(3, Duration.ofSeconds(1))
        .maxBackoff(Duration.ofSeconds(5))
        .filter(this::isRetryable)
        .onRetryExhaustedThrow((signalSpec, signal) -> signal.failure());
  }

  private boolean isRetryable(Throwable t) {
    if (t instanceof ExternalApiException e) {
      int status = e.getStatusCode();
      return status == 429 || status >= 500;
    }
    if (t instanceof WebClientRequestException) {
      return true; // network-level failure, safe to retry
    }
    return false;
  }

  private Mono<ExternalApiException> error(
      String message, ExternalApiSource source, HttpStatusCode statusCode) {
    return Mono.error(new ExternalApiException(message, source, statusCode.value()));
  }
}
