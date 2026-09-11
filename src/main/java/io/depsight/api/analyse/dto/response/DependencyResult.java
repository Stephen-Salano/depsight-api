package io.depsight.api.analyse.dto.response;

import java.util.List;

/**
 * Represents a single dependency node in a dependency tree.
 * It has coordinates
 * @param groupId
 * @param artifactId
 * @param version
 * @param scope
 * @param depth in the tree,
 * @param size info
 * @param vulnerabilities
 * @param children dependencies.
 */
public record DependencyResult(
        String groupId,
        String artifactId,
        String version,
        String scope,
        int depth,
        Long sizeInBytes,
        String size,
        List<VulnerabilityResult> vulnerabilities,
        List<DependencyResult> children) {

    public DependencyResult {
        if (vulnerabilities == null) {
            vulnerabilities = List.of();
        }
        if (children == null) {
            children = List.of();
        }
    }
}
