package io.depsight.api.analyse.parser;

import io.depsight.api.analyse.dto.request.MavenCooridinates;
import io.depsight.api.analyse.dto.request.ParsedDependency;
import io.depsight.api.common.exception.BadRequestException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

@RequiredArgsConstructor
@Slf4j
public class PomParser {

  private static String UNRESOLVED = "UNRESOLVED";

  /**
   * The parse method returns a model now becuase AnalyseServiceImpl now needs to do more with the
   * parsed data(extract deps, extract he parent pointer) Returning a model lets the {@link
   * io.depsight.api.analyse.service.AnalyseServiceImpl} do what it needs Model will now hold the
   * entire pom as a Java Object
   *
   * @param pomXml The pomxml string pasted by the user
   * @return a Model object the raw parsed pom.xml that contains everything dependencies,
   *     properties, parent info etc.
   * @throws BadRequestException
   */
  public static Model parse(String pomXml) throws BadRequestException {
    log.info("cleaning pom file");
    String cleanedPom = cleanPom(pomXml);
    validatePom(cleanedPom);
    MavenXpp3Reader reader = new MavenXpp3Reader();
    Model model;
    try {
      model = reader.read(new StringReader(cleanedPom));
    } catch (XmlPullParserException | IOException e) {
      log.error("Failed to parse POM: {}", e.getMessage());
      throw new BadRequestException("Invalid POM structure" + e.getMessage());
    }
    return model;
  }

  public static MavenCooridinates extractParent(Model model) {
    Parent parent = model.getParent();

    if (parent == null) {
      return null;
    }
    return new MavenCooridinates(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
  }

  /**
   * Turns a model Dependency into a {@link ParsedDependency} It does not fetch anything from maven
   * central repo it simply reads the user's XML
   *
   * @param model Java object of the pom.xml
   * @param properties a map of pom.xml properties section
   * @return list of {@link ParsedDependency}
   */
  public static List<ParsedDependency> extractDependencies(
      Model model, Map<String, String> properties) {
    // get deps
    List<Dependency> parsedDependencies = model.getDependencies();
    List<ParsedDependency> result = new ArrayList<>();
    String groupId, artifactId, version, scope;
    for (Dependency dependency : parsedDependencies) {
      // get each dependency's group, artifacst and version
      groupId = dependency.getGroupId();
      artifactId = dependency.getArtifactId();
      version = dependency.getVersion();
      scope = dependency.getScope();

      // if the version is a place holder we try and see if we can fetch from properties section
      if (version != null && version.startsWith("${")) {
        String key = version.substring(2, version.length() - 1);
        version = properties.getOrDefault(key, UNRESOLVED);
      }
      // if the version came in null, just mark as UNRESOLVED
      if (version == null) {
        version = UNRESOLVED;
      }

      // add the new ParsedDependency to the empty list
      result.add(new ParsedDependency(groupId, artifactId, version, scope));
    }
    return result;
  }

  /**
   * Some poms define version numbers as variables
   * ``<org.mapstruct.version>1.6.3</org.mapstruct.version>` Then use them like
   * `<version>${org.mapstruct.version}</version>` This method reads the properties block and builds
   * a map: "org.mapstruct.version" → "1.6.3"
   *
   * @param model the parsed pom.xml now as a Java object
   * @return A map of properties
   */
  public static Map<String, String> extractProperties(Model model) {
    Map<String, String> properties = new HashMap<>();
    if (!model.getProperties().isEmpty()) {
      model
          .getProperties()
          .forEach((key, value) -> properties.put(key.toString(), value.toString()));
    }
    return properties;
  }

  /**
   * Scans springboot dependencies and pulls out the import entries (like micrometer), resolving
   * microemter version of microemter.version -> 1.16.5 using properties
   *
   * @param model the raw parsed pom.xml
   * @param properties all the dependency versions listed in a properties section in a map
   * @return a List of traversed dependency nodes
   */
  public static List<MavenCooridinates> extractImports(
      Model model, Map<String, String> properties) {
    List<MavenCooridinates> cooridinates = new ArrayList<>();

    if (model.getDependencyManagement() == null) {
      return cooridinates;
    }
    List<Dependency> extractedDeps = model.getDependencyManagement().getDependencies();

    String parentGroupId, parentArtifactId, parentVersion, scope, type;
    for (Dependency dep : extractedDeps) {
      parentGroupId = dep.getGroupId();
      parentArtifactId = dep.getArtifactId();
      parentVersion = dep.getVersion();
      scope = dep.getScope();
      type = dep.getType();

      if (type.equals("pom") && scope.equals("import")) {
        if (parentVersion == null) {
          log.info(
              "import {groupId}:{artifactId} has no version, marking UNRESOLVED",
              parentGroupId,
              parentArtifactId);
          parentVersion = UNRESOLVED;
        }
        if (parentVersion.startsWith("${")) {
          String key = parentVersion.substring(2, parentVersion.length() - 1);
          parentVersion = properties.getOrDefault(key, UNRESOLVED);
        }
      } else {
        continue;
      }
      cooridinates.add(new MavenCooridinates(parentGroupId, parentArtifactId, parentVersion));
    }
    return cooridinates;
  }

  private static void validatePom(String cleanedPom) throws BadRequestException {
    // declare list
    ArrayList<String> errors = new ArrayList<String>();
    // 5 checks
    if (!cleanedPom.contains("<project") || !cleanedPom.contains("</project")) {
      errors.add("Missing <project> definitions");
    }
    if (!cleanedPom.contains("<modelVersion>") || !cleanedPom.contains("</modelVersion")) {
      errors.add("Missing <modelVersion> tags");
    }
    if (!cleanedPom.contains("<groupId>") || !cleanedPom.contains("</groupId>")) {
      errors.add("Missing <groupId> tags");
    }
    if (!cleanedPom.contains("<artifactId>") || !cleanedPom.contains("</artifactId>")) {
      errors.add("Missing <artifactId> tags");
    }
    if (!cleanedPom.contains("<version>") || !cleanedPom.contains("</version>")) {
      errors.add("Missing <version> tags");
    }

    // Throw incase there's errors
    if (!errors.isEmpty()) {
      throw new BadRequestException(String.join(", ", errors));
    }
  }

  /**
   * This code checks whether a string starts with an XML declaration The reason for this is so that
   *
   * <p>the MavenXpp3Reader doesn't throw error indexOf Find where "?>" starts, skip both characters
   * (? and >), then take everything after them. + 2 becuase "?>" consists of two characters so
   * basically ">?" (start from the end) substring return everything after the ending of pom.xml
   * "?>" stripLeading removes whitespace leftover
   *
   * @param pomXml the pasted pom.xml string
   * @return a pom.xml string without the xml declaration
   */
  private static String cleanPom(String pomXml) {
    // Checks whether pom.xml starts with the xml declaration
    if (pomXml.startsWith("<?xml")) {
      return pomXml.substring(pomXml.indexOf("?>") + 2).stripLeading();
    } else {
      return pomXml;
    }
  }
}
