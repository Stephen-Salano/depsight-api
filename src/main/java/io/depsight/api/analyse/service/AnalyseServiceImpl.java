package io.depsight.api.analyse.service;

import io.depsight.api.analyse.dto.request.AnalyseRequest;
import io.depsight.api.analyse.dto.request.MavenCooridinates;
import io.depsight.api.analyse.dto.request.ParsedDependency;
import io.depsight.api.analyse.parser.PomParser;
import io.depsight.api.analyse.resolver.ParentBomResolver;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.model.Model;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyseServiceImpl implements AnalyseService {

  private final ParentBomResolver parentBomResolver;

  @Override
  public List<ParsedDependency> analyse(AnalyseRequest request) {
    // extract the string and call Pom parser static method

    log.info("Parsing Pom from request");
    Model model = PomParser.parse(request.pomXml());

    Map<String, String> properties = PomParser.extractProperties(model);
    List<ParsedDependency> dependencies = PomParser.extractDependencies(model, properties);
    // Get the parent from the pomXml model;
    MavenCooridinates cooridinates = PomParser.extractParent(model);
    if (cooridinates == null) {
      return dependencies; // no parent bom
    }
    List<ParsedDependency> resolved = parentBomResolver.resolveParent(cooridinates, dependencies);
    return resolved;
  }
}
