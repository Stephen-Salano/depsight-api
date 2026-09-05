package io.depsight.api.analyse.dto.response;

import io.depsight.api.analyse.resolver.DependencyNode;
import io.depsight.api.analyse.resolver.dto.VersionRequest;
import java.util.List;
import java.util.Map;

public record ResolutionResult(List<DependencyNode> tree, Map<String, List<VersionRequest>> versionRequest) {}
