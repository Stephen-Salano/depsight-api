package io.depsight.api.analyse.resolver;

import java.util.List;

/**
 * Represents one node in the depdnency tree
 *
 * @param groupId
 * @param artifactId
 * @param version
 * @param scope
 * @param depth
 * @param children
 */
public record DependencyNode(
    String groupId,
    String artifactId,
    String version,
    String scope,
    int depth,
    List<DependencyNode> children) {}
