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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Helps resolve dependencies marked as "UNRESOLVED" by {@link PomParser} parse() method - Fetches
 * the parent BOM from maven central repo using the MavenCentralClient - Finds the unresolved
 * version - returs a clean ParsedDependency
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class ParentBomResolver {

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

  public Mono<Map<String, String>> resolveBomImports(
      List<MavenCooridinates> imports, Map<String, String> properties) {

    return Flux.fromIterable(imports)
        .flatMap(
            coord ->
                mavenCentralClient
                    .fetchPomXml(
                        coord.parentGroupId(), coord.parentArtifactId(), coord.parentVersion())
                    .map(
                        pomXml -> {
                          Model model = PomParser.parse(pomXml);
                          Map<String, String> resolvedProps = PomParser.extractProperties(model);
                          return extractBom(model, resolvedProps);
                        }))
        .collectList()
        .map(
            bomList -> {
              Map<String, String> merged = new HashMap<>();
              for (Map<String, String> bom : bomList) {
                merged.putAll(bom);
              }
              return merged;
            });
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
}
