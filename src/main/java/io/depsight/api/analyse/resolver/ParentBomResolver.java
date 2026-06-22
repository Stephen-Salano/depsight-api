package io.depsight.api.analyse.resolver;

import io.depsight.api.analyse.dto.request.MavenCooridinates;
import io.depsight.api.analyse.dto.request.ParsedDependency;
import io.depsight.api.analyse.parser.PomParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;

@RequiredArgsConstructor
@Slf4j
public class ParentBomResolver {
  // TODO: Add @Service when we connect it to MavenCentralClient => turn it into a Spring class

  private static final String UNRESOLVED = "UNRESOLVED";
  private static final String BASE_URL = "https://repo1.maven.org/maven2/";

  public static List<ParsedDependency> resolveParent(
      MavenCooridinates cooridinates, List<ParsedDependency> dependencies) {
    String pomXml =
        fetchPomXml(
            cooridinates.parentGroupId(),
            cooridinates.parentArtifactId(),
            cooridinates.parentVersion());

    Model model = PomParser.parse(pomXml);

    Map<String, String> properties = PomParser.extractProperties(model);

    Map<String, String> bom = extractBom(model, properties);

    if (bom.isEmpty() && model.getParent() != null) {
      MavenCooridinates parentCoordinates = PomParser.extractParent(model);

      String parentPomXml =
          fetchPomXml(
              parentCoordinates.parentGroupId(),
              parentCoordinates.parentArtifactId(),
              parentCoordinates.parentVersion());

      Model parentModel = PomParser.parse(parentPomXml);
      Map<String, String> parentProperties = PomParser.extractProperties(parentModel);
      Map<String, String> parentBom = extractBom(parentModel, parentProperties);

      bom.putAll(parentBom);
    }

    return applyBom(dependencies, bom);
  }

  private static List<ParsedDependency> applyBom(
      List<ParsedDependency> dependencies, Map<String, String> bom) {
    List<ParsedDependency> result = new ArrayList<>();

    for (ParsedDependency dependency : dependencies) {
      if (dependency.version().equals(UNRESOLVED)) {
        String resolved = bom.get(dependency.groupId() + ":" + dependency.artifactId());
        if (resolved != null) {
          // create a new dependency with the resolved version
          result.add(
              new ParsedDependency(
                  dependency.groupId(), dependency.artifactId(), resolved, dependency.scope()));
        } else {
          result.add(dependency); // unresolved so kept in bom
        }
      } else {
        result.add(dependency);
      }
    }
    return result;
  }

  /**
   * @param model the pomxml string turned into a model for us to get the Versions
   * @param properties the properties section in pomxml
   * @return a Map of Dependency and the version value
   */
  private static Map<String, String> extractBom(Model model, Map<String, String> properties) {
    Map<String, String> bom = new HashMap<>();

    if (model.getDependencyManagement() == null) {
      return bom;
    }
    List<Dependency> managed = model.getDependencyManagement().getDependencies();

    for (Dependency dependency : managed) {
      String version = dependency.getVersion();

      if (version != null && version.startsWith("${")) {
        String propertyKey = version.substring(2, version.length() - 1);
        version = properties.getOrDefault(propertyKey, UNRESOLVED);
      }
      String bomKey = dependency.getGroupId() + ":" + dependency.getArtifactId();
      bom.put(bomKey, version);
    }
    return bom;
  }

  private static String fetchPomXml(
      String parentGroupId, String parentArtifactId, String parentVersion) {
    String groupPath = parentGroupId.replace(".", "/");

    try {
      String url =
          BASE_URL
              + groupPath
              + "/"
              + parentArtifactId
              + "/"
              + parentVersion
              + "/"
              + parentArtifactId
              + "-"
              + parentVersion
              + ".pom";

      HttpClient client = HttpClient.newHttpClient();
      HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200) {
        throw new RuntimeException("Failed to fetch BOM: " + response.statusCode());
      }
      return response.body();
    } catch (Exception e) {
      throw new RuntimeException("Error fetching BOM POM", e);
    }
  }
}
