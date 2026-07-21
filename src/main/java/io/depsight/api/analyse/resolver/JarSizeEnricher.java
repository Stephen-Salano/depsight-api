package io.depsight.api.analyse.resolver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Take an existing dependency tree and enrich it with additional metadata Flatten the tree. Remove
 * duplicates. Fetch JAR sizes. Attach those sizes back onto the tree.
 */
@Service
@Slf4j
public class JarSizeEnricher {

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
}
