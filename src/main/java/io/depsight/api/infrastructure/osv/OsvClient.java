package io.depsight.api.infrastructure.osv;

import io.depsight.api.common.enums.ExternalApiSource;
import io.depsight.api.common.exception.ExternalApiException;
import io.depsight.api.infrastructure.osv.dto.request.BatchRequest;
import io.depsight.api.infrastructure.osv.dto.response.BatchResponse;
import io.depsight.api.infrastructure.osv.dto.response.OsvVulnerability;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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

    // TODO: Add timeout, retry/backoff for transient failures, and map WebClient/Jackson errors to
    // ExternalApiException
    return client
        .post() // build the request
        .uri(QUERYBATCH_URL) // where to
        .bodyValue(request) // put the request inside
        .retrieve() // fire off the request
        .onStatus(
            status -> status.isError(),
            resp -> Mono.error(new ExternalApiException(BATCH_MESSAGE, ExternalApiSource.OSV_DEV)))
        .bodyToMono(BatchResponse.class); // what comes back
  }

  public Mono<OsvVulnerability> fetchVulnerability(String id) {

    return client
        .get()
        .uri(VULNERABILITY_URL, id) // webclient will replace the id in the endpoint
        .retrieve()
        .onStatus(
            status -> status.isError(),
            resp -> Mono.error(new ExternalApiException(VULN_MESSAGE, ExternalApiSource.OSV_DEV)))
        .bodyToMono(OsvVulnerability.class);
  }
}
