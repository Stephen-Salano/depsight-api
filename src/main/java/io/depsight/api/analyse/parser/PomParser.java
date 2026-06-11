package io.depsight.api.analyse.parser;

import io.depsight.api.analyse.dto.request.ParsedDependency;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

@Getter
@Setter
@RequiredArgsConstructor
@Slf4j
public class PomParser {

  private static List<ParsedDependency> parse(String pomXml) {
    log.info("cleaning pom file");
    String cleanedPom = cleanPom(pomXml);
    validatePom(cleanedPom);
    MavenXpp3Reader reader = new MavenXpp3Reader();
    Model model = reader.read(new StringReader(cleanedPom));
    Map<String, String> properties = extractProperties(model);
    List<ParsedDependency> deps = extractDependencies(model, properties);
    return deps;
  }

  private static List<ParsedDependency> extractDependencies(
      Model model, Map<String, String> properties) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'extractDependencies'");
  }

  private static Map<String, String> extractProperties(Model model) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'extractProperties'");
  }

  private static void validatePom(String cleanedPom) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'validatePom'");
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
