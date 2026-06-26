package io.depsight.api.analyse.resolver;

import java.util.List;

public record DependencyNode(
    String groupId,
    String artifactId,
    String version,
    String scope,
    int depth,
    List<DependencyNode> children) {}
