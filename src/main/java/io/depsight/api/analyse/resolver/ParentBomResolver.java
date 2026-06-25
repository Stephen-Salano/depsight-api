package io.depsight.api.analyse.resolver;

import io.depsight.api.analyse.dto.request.MavenCooridinates;
import io.depsight.api.analyse.dto.request.ParsedDependency;
import io.depsight.api.analyse.parser.PomParser;
import io.depsight.api.common.exception.ResourceNotFoundException;
import io.depsight.api.infrastructure.maven.MavenCentralClient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class ParentBomResolver {
  // TODO: Add @Service when we connect it to MavenCentralClient => turn it into a Spring class

  private static final String UNRESOLVED = "UNRESOLVED";
  // private static final String BASE_URL = "https://repo1.maven.org/maven2/";
  private final MavenCentralClient mavenCentralClient;

  /**
   * This is the main resolver method. Takes a users resolved pom as {@link ParsedDependency} and
   * MavenCooridinates Creates a link using MavenCooridinates fetches the parent BOM pom from
   * maven's Repository Extracts the properties from the parent bom pom Replaced UNRESOLVED
   * dependencies versions with fetched parent bom pom
   *
   * @param cooridinates Parent BOM Details {@link MavenCooridinates}
   * @param dependencies Users parsed Dependecies from the pasted POM
   * @return A list of {@link ParsedDependency}
   */
  public List<ParsedDependency> resolveParent(
      MavenCooridinates cooridinates, List<ParsedDependency> dependencies) {
    String pomXml =
        mavenCentralClient
            .fetchPomXml(
                cooridinates.parentGroupId(),
                cooridinates.parentArtifactId(),
                cooridinates.parentVersion())
            .block(); // Sequential calls so okay to block

    if (pomXml == null) {
      throw new ResourceNotFoundException(
          "Parent Pom not found: " + cooridinates.parentArtifactId());
    }

    Model model = PomParser.parse(pomXml);

    Map<String, String> properties = PomParser.extractProperties(model);

    Map<String, String> bom = extractBom(model, properties);

    if (bom.isEmpty() && model.getParent() != null) {
      MavenCooridinates parentCoordinates = PomParser.extractParent(model);

      String parentPomXml =
          mavenCentralClient
              .fetchPomXml(
                  parentCoordinates.parentGroupId(),
                  parentCoordinates.parentArtifactId(),
                  parentCoordinates.parentVersion())
              .block(); // Sequential calls so okay to block

      if (parentPomXml == null) {
        throw new ResourceNotFoundException(
            "Parent Pom not found: " + parentCoordinates.parentArtifactId());
      }
      Model parentModel = PomParser.parse(parentPomXml);
      Map<String, String> parentProperties = PomParser.extractProperties(parentModel);
      Map<String, String> parentBom = extractBom(parentModel, parentProperties);

      bom.putAll(parentBom);
    }

    return applyBom(dependencies, bom);
  }

  /**
   * This method takes all dependencies and looks up dependencies with "UNRESOLVED" versions and
   * replaces them with Parent BOM versions
   *
   * @param dependencies The users pasted pom.xml
   * @param bom the parent bom pom
   * @return List of {@link io.depsight.api.analyse.dto.request.ParsedDependency}
   */
  private List<ParsedDependency> applyBom(
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
  private Map<String, String> extractBom(Model model, Map<String, String> properties) {
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

  // /**
  //  * This method uses the {@link MavenCooridinates} to form a link to fetch the pomxml from the
  //  * maven repo
  //  *
  //  * @param parentGroupId the parent groupId
  //  * @param parentArtifactId the parent's artifact Id needed for the url
  //  * @param parentVersion the version of the parent pom needed for the url:with
  //  * @return {@link String} of the fetched parent pom.xml
  //  */
  // private static String fetchPomXml(
  //     String parentGroupId, String parentArtifactId, String parentVersion) {
  //   // replacing the "." with "/" becuase the maven repo uses "/" directories.
  //   // so org.springframwork.boot => org/springframwork/boot which is what we actually need
  //   String groupPath = parentGroupId.replace(".", "/");
  //
  //   try {
  //     // Creating the url from the MavenCooridinates
  //     String url =
  //         BASE_URL
  //             + groupPath //  org.springframwork.boot => org/springframwork/boot
  //             + "/" // org/springframwork/boot/
  //             + parentArtifactId // org/springframwork/boot/spring-boot-starter-parent
  //             + "/" // org/springframwork/boot/spring-boot-starter-parent/
  //             + parentVersion // org/springframwork/boot/spring-boot-starter-parent/4.0.6
  //             + "/" // org/springframwork/boot/spring-boot-starter-parent/4.0.6/
  //             + parentArtifactId //
  // org/springframwork/boot/spring-boot-starter-parent/4.0.6/spring-boot-starter-parent
  //             + "-" //
  // org/springframwork/boot/spring-boot-starter-parent/4.0.6/spring-boot-starter-parent-
  //             + parentVersion //
  // org/springframwork/boot/spring-boot-starter-parent/4.0.6/spring-boot-starter-parent-4.0.6
  //             + ".pom"; //
  // org/springframwork/boot/spring-boot-starter-parent/4.0.6/spring-boot-starter-parent-4.0.6.pom
  // (the full link)
  //
  //     // Create a Java Http client to fetch the pom using the url above
  //     HttpClient client = HttpClient.newHttpClient();
  //     HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
  //
  //     // Send the request using the url and calling the GET method which returns the entire
  // parent
  //     // pom bom as a String
  //     HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
  //
  //     if (response.statusCode() != 200) {
  //       throw new RuntimeException("Failed to fetch BOM: " + response.statusCode());
  //     }
  //     return response.body();
  //   } catch (Exception e) {
  //     throw new RuntimeException("Error fetching BOM POM", e);
  //   }
  // }
}
