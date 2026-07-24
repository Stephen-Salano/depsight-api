package io.depsight.api.analyse.dto.response;

import java.util.List;

public record DependencyResult(
    String groupId,
    String artifactId,
    String version,
    String scope,
    int depth,
    Long sizeInBytes,
    String size,
    List<DependencyResult> children) {}
