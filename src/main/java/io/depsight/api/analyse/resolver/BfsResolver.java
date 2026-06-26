package io.depsight.api.analyse.resolver;

import io.depsight.api.analyse.dto.request.ParsedDependency;
import io.depsight.api.analyse.parser.PomParser;
import io.depsight.api.infrastructure.maven.MavenCentralClient;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.model.Model;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class BfsResolver {

  private final MavenCentralClient mavenCentralClient;
  private static final int MAX_DEPTH = 6;
  private static final String UNRESOLVED = "UNRESOLVED";

  public List<DependencyNode> resolve(List<ParsedDependency> directDeps) {
    Set<String> visited = new HashSet<>();
    List<DependencyNode> nextLevl =
        Flux.fromIterable(directDeps)
            .flatMap(
                dep -> {
                  if (!UNRESOLVED.equals(dep.version())
                      && !"test".equals(dep.scope())
                      && !"provided.".equals(dep.scope())
                      && !visited.contains(dep.groupId() + ":" + dep.artifactId())) {
                    visited.add(dep.groupId() + ":" + dep.artifactId());
                    return mavenCentralClient
                        .fetchPomXml(dep.groupId(), dep.artifactId(), dep.version())
                        .map(
                            pomXml -> {
                              log.info(
                                  "Fetched POM for {}:{} = {}",
                                  dep.groupId(),
                                  dep.artifactId(),
                                  pomXml.substring(0, Math.min(100, pomXml.length())));
                              Model pomModel = PomParser.parse(pomXml);
                              Map<String, String> properties =
                                  PomParser.extractProperties(pomModel);
                              List<ParsedDependency> childrenDeps =
                                  PomParser.extractDependencies(pomModel, properties);
                              List<DependencyNode> nodes =
                                  childrenDeps.stream()
                                      .map(
                                          childDep ->
                                              new DependencyNode(
                                                  childDep.groupId(),
                                                  childDep.artifactId(),
                                                  childDep.version(),
                                                  childDep.scope(),
                                                  1,
                                                  List.of()))
                                      .toList();
                              return new DependencyNode(
                                  dep.groupId(),
                                  dep.artifactId(),
                                  dep.version(),
                                  dep.scope(),
                                  0,
                                  nodes);
                            });
                  } else {
                    return Mono.empty();
                  }
                })
            .collectList()
            .block();
    return nextLevl;
  }
}
