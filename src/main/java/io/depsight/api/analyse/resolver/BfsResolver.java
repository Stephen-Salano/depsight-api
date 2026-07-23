package io.depsight.api.analyse.resolver;

import io.depsight.api.analyse.dto.request.MavenCooridinates;
import io.depsight.api.analyse.dto.request.ParsedDependency;
import io.depsight.api.analyse.parser.PomParser;
import io.depsight.api.infrastructure.maven.MavenCentralClient;
import java.time.Duration;
import java.util.ArrayList;
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

/** Given a list of direct dependencies, we need to build a dependency tree */
@Service
@Slf4j
@RequiredArgsConstructor
public class BfsResolver {

  private final MavenCentralClient mavenCentralClient;
  private static final int MAX_ALLOWED_DEPTH = 6;
  private static final String UNRESOLVED = "UNRESOLVED";
  private final ParentBomResolver parentBomResolver;

  /**
   * Does one flatmap pass, builds children as flat DependencyNode
   *
   * @param directDeps
   * @return
   */
  public List<DependencyNode> resolve(List<ParsedDependency> directDeps, int maxDepth) {
    int cappedDepth = Math.min(maxDepth, MAX_ALLOWED_DEPTH);
    Set<String> visited = new HashSet<>();
    return Flux.fromIterable(directDeps)
        .flatMap(dep -> resolveNode(dep, 0, cappedDepth, visited))
        .collectList()
        .timeout(Duration.ofSeconds(15))
        .block();
  }

  private Mono<DependencyNode> resolveNode(
      ParsedDependency dependency, int depth, int maxDepth, Set<String> visited) {
    // guard checks
    if (UNRESOLVED.equals(dependency.version())
        || "test".equals(dependency.scope())
        || "provided".equals(dependency.scope())
        || visited.contains(dependency.groupId() + ":" + dependency.artifactId())) {
      return Mono.empty();
    }

    visited.add(dependency.groupId() + ":" + dependency.artifactId());
    // The depth cap ()
    if (depth >= maxDepth) {
      return Mono.just(
          new DependencyNode(
              dependency.groupId(),
              dependency.artifactId(),
              dependency.version(),
              dependency.scope(),
              depth,
              List.of()));
    }

    // Why FlatMap(), becuase our lambda returns Mono<Map<String, String>>
    return mavenCentralClient
        .fetchPomXml(dependency.groupId(), dependency.artifactId(), dependency.version())
        .flatMap(
            pomXml -> {
              Model model = PomParser.parse(pomXml);
              Map<String, String> properties = PomParser.extractProperties(model);
              List<ParsedDependency> childrenDeps =
                  PomParser.extractDependencies(model, properties);

              List<MavenCooridinates> extractedImports =
                  PomParser.extractImports(model, properties);

              return parentBomResolver
                  .resolveBomImports(extractedImports, properties)
                  .flatMap(
                      resolvedImports -> {
                        List<ParsedDependency> resolvedChildrenDeps = new ArrayList<>();
                        for (ParsedDependency dep : childrenDeps) {
                          if (dep.version().equals(UNRESOLVED)) {
                            String version =
                                resolvedImports.get(dep.groupId() + ":" + dep.artifactId());
                            if (version != null) {
                              resolvedChildrenDeps.add(
                                  new ParsedDependency(
                                      dep.groupId(), dep.artifactId(), version, dep.scope()));
                            } else {
                              resolvedChildrenDeps.add(
                                  new ParsedDependency(
                                      dep.groupId(), dep.artifactId(), UNRESOLVED, dep.scope()));
                            }
                          } else {
                            resolvedChildrenDeps.add(
                                new ParsedDependency(
                                    dep.groupId(), dep.artifactId(), dep.version(), dep.scope()));
                          }
                        }

                        Mono<List<DependencyNode>> childrenMono =
                            Flux.fromIterable(resolvedChildrenDeps)
                                .flatMap(
                                    childDep -> resolveNode(childDep, depth + 1, maxDepth, visited))
                                .collectList();

                        return childrenMono.map(
                            children ->
                                new DependencyNode(
                                    dependency.groupId(),
                                    dependency.artifactId(),
                                    dependency.version(),
                                    dependency.scope(),
                                    depth,
                                    children));
                      });
            });
  }
}
