package io.depsight.api.infrastructure.maven;

import io.depsight.api.common.enums.ExternalApiSource;
import io.depsight.api.common.exception.ExternalApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

@Service
@Slf4j
public class MavenCentralClient {

  private final WebClient client;

  public MavenCentralClient(WebClient.Builder builder) {
    this.client = builder.baseUrl("https://repo1.maven.org/maven2/").build();
  }

  public Mono<String> fetchPomXml(String groupId, String artifactId, String version) {
    String key =
        groupId
            + ":"
            + artifactId
            + ":"
            + version; // stores our url so our log looks like : Fetched
    // org.springframework:spring-core:7.0.7 in 312ms
    return client
        .get()
        .uri(buildUrl(groupId, artifactId, version))
        .retrieve()
        .onStatus(status -> status.value() == 404, resp -> Mono.empty())
        .onStatus(
            status -> status.isError(),
            resp ->
                Mono.error(
                    new ExternalApiException(
                        "Something went wrong while fetching", ExternalApiSource.MAVEN_CENTRAL)))
        .bodyToMono(String.class)
        .elapsed()
        .doOnNext(tuple -> log.info("Fetched {} in {}ms", key, tuple.getT1()))
        .map(Tuple2::getT2)
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
