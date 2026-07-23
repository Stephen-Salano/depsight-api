package io.depsight.api.analyse.resolver;

import io.depsight.api.common.exception.ExternalApiException;
import io.depsight.api.infrastructure.maven.MavenCentralClient;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Take an existing dependency tree and enrich it with additional metadata Flatten the tree. Remove
 * duplicates. Fetch JAR sizes. Attach those sizes back onto the tree.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class JarSizeEnricher {

  private final MavenCentralClient mavenCentralClient;

  /**
   * This method calls a recursive helper method that checks if each node is in a flatened list
   *
   * @param tree DependencyNode tree contaiing parent-child deps
   * @return a flattened list of unique DependencyNodes
   */
  private List<DependencyNode> flatten(List<DependencyNode> tree) {
    // create the shared visited set
    Set<String> visited = new HashSet<>();
    // Create the shared flattened List
    List<DependencyNode> flattenedList = new ArrayList<>();
    // iterate over every root node in the tree
    for (DependencyNode node : tree) {
      flattenNode(node, visited, flattenedList);
    }
    log.info("Flattened tree into {} unique dependencies", flattenedList.size());
    return flattenedList;
  }

  /**
   * Recursive method that checks if a unique DependencyNode is part of our flattened list
   *
   * @param node a single DependencyNode from the tree
   * @param visited set of visited nodes
   * @param flattenedList list of individual deps
   */
  private void flattenNode(
      DependencyNode node, Set<String> visited, List<DependencyNode> flattenedList) {
    String key = node.groupId() + ":" + node.artifactId() + ":" + node.version();
    // guard close
    if (visited.contains(key)) {
      return;
    }
    visited.add(key);
    flattenedList.add(node);
    for (DependencyNode child : node.children()) {
      flattenNode(child, visited, flattenedList);
    }
  }

  private Mono<Map<String, Long>> fetchJarSizes(List<DependencyNode> flatNodes) {
    return Flux.fromIterable(flatNodes)
        .flatMap(
            node -> {
              String key = coordinateKey(node);
              return mavenCentralClient
                  .fetchJarSize(node.groupId(), node.artifactId(), node.version())
                  .onErrorResume(ExternalApiException.class, error -> Mono.empty())
                  .map(size -> Map.entry(key, size));
            })
        .collectMap(Map.Entry::getKey, Map.Entry::getValue);
  }

  private String coordinateKey(DependencyNode node) {
    return node.groupId() + ":" + node.artifactId() + ":" + node.version();
  }
}
