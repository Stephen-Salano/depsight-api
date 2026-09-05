package io.depsight.api.analyse.dto.response;

import io.depsight.api.analyse.resolver.DependencyNode;
import io.depsight.api.analyse.resolver.dto.VersionRequest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record ResolutionResult(List<DependencyNode> tree, Map<String, List<VersionRequest>> versionRequest) {
    public ResolutionResult {
        tree = List.copyOf(tree);
        versionRequest = Map.copyOf(versionRequest.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> List.copyOf(e.getValue()))));
    }
}
