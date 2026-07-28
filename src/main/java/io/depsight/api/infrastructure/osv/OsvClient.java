package io.depsight.api.infrastructure.osv;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
public class OsvClient {

  private final WebClient client;

  public OsvClient(WebClient.Builder builder) {
    this.client = builder.baseUrl("https://api.osv.dev").build();
  }
}
