package io.depsight.api.analyse.parser;

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
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

@RequiredArgsConstructor
@Slf4j
public class PomParser {

  private static String UNRESOLVED = "Unresolved";

  public static List<ParsedDependency> parse(String pomXml) throws BadRequestException {
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
    Map<String, String> properties = extractProperties(model);
    List<ParsedDependency> deps = extractDependencies(model, properties);
    return deps;
  }

  private static List<ParsedDependency> extractDependencies(
      Model model, Map<String, String> properties) {
    // get deps
    List<Dependency> parsedDependencies = model.getDependencies();
    List<ParsedDependency> result = new ArrayList<>();
    String groupId, artifactId, version, scope;
    for (Dependency dependency : parsedDependencies) {
      groupId = dependency.getGroupId();
      artifactId = dependency.getArtifactId();
      version = dependency.getVersion();
      scope = dependency.getScope();

      if (version != null && version.startsWith("${")) {
        String key = version.substring(2, version.length() - 1);
        version = properties.getOrDefault(key, UNRESOLVED);
      }
      if (version == null) {
        version = UNRESOLVED;
      }

      result.add(new ParsedDependency(groupId, artifactId, version, scope));
    }
    return result;
  }

  private static Map<String, String> extractProperties(Model model) {
    Map<String, String> properties = new HashMap<>();
    if (!model.getProperties().isEmpty()) {
      model
          .getProperties()
          .forEach((key, value) -> properties.put(key.toString(), value.toString()));
    }
    return properties;
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
