package io.depsight.api.infrastructure.maven;

import io.depsight.api.common.exception.ExternalApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class MavenCentralClient {

  private final WebClient client;

  public MavenCentralClient(WebClient.Builder builder) {
    this.client = builder.baseUrl("https://repo1.maven.org/maven2/").build();
  }

  public Mono<String> fetchPomXml(String groupId, String artifactId, String version) {
    return client
        .get()
        .uri(buildUrl(groupId, artifactId, version))
        .retrieve()
        .onStatus(status -> status.value() == 404, resp -> Mono.empty())
        .onStatus(
            status -> status.isError(),
            resp -> Mono.error(new ExternalApiException("Something went wrong while fetching")))
        .bodyToMono(String.class)
        .onErrorResume(ExternalApiException.class, e -> Mono.error(e));
  }

  private String buildUrl(String groupId, String artifactId, String version) {

    String groupPath = groupId.replace(".", "/");
    // Creating the url from the MavenCooridinates
    return groupPath //  org.springframwork.boot => org/springframwork/boot
        + "/" // org/springframwork/boot/
        + artifactId // org/springframwork/boot/spring-boot-starter-parent
        + "/" // org/springframwork/boot/spring-boot-starter-parent/
        + version // org/springframwork/boot/spring-boot-starter-parent/4.0.6
        + "/" // org/springframwork/boot/spring-boot-starter-parent/4.0.6/
        + artifactId // org/springframwork/boot/spring-boot-starter-parent/4.0.6/spring-boot-starter-parent
        + "-" // org/springframwork/boot/spring-boot-starter-parent/4.0.6/spring-boot-starter-parent-
        + version // org/springframwork/boot/spring-boot-starter-parent/4.0.6/spring-boot-starter-parent-4.0.6
        + ".pom"; // org/springframwork/boot/spring-boot-starter-parent/4.0.6/spring-boot-starter-parent-4.0.6.pom (the full link)
  }
}
