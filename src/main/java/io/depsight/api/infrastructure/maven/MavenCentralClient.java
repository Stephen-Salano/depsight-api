package io.depsight.api.infrastructure.maven;

import org.springframework.stereotype.Component;

@Component
public class MavenCentralClient {
  private static final String BASE_URL = "https://repo1.maven.org/maven2/";
  // private final HttpClient client = HttpClient.newBuilder
  // public String fetchPom(MavenCooridinates cooridinates) {
  //   String url = buildUrl(cooridinates);
  //   String fetchedPom = httpGet(url);
  // }
  //
  // private String httpGet(String url) {
  //
  // }
  //
  // private String buildUrl(MavenCooridinates cooridinates) {
  //   String groupPath = cooridinates.parentGroupId().replace(".", "/");
  //
  //   return BASE_URL
  //       + groupPath
  //       + "/"
  //       + cooridinates.parentArtifactId()
  //       + "/"
  //       + cooridinates.parentVersion()
  //       + "/"
  //       + cooridinates.parentArtifactId()
  //       + "-"
  //       + cooridinates.parentVersion()
  //       + ".pom";
  // }
}
