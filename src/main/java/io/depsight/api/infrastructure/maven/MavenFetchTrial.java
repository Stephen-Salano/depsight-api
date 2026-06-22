package io.depsight.api.infrastructure.maven;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class MavenFetchTrial {
  public static void main(String[] args) throws Exception {
    String groupId = "org.postgresql";
    String artifactId = "postgresql";
    String version = "42.7.3";

    // Step 1: Build the URL
    String groupPath = groupId.replace(".", "/");
    String url =
        "https://repo1.maven.org/maven2/"
            + groupPath
            + "/"
            + artifactId
            + "/"
            + version
            + "/"
            + artifactId
            + "-"
            + version
            + ".pom";

    System.out.println("Fetching: " + url);

    // Step 2: Fetch it - plaom Java
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    System.out.println("Status: " + response.statusCode());
    System.out.println("Body: \n" + response.body());
  }
}
