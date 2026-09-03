package io.depsight.api.analyse.resolver;

import io.depsight.api.analyse.dto.response.AnalysisResult;
import io.depsight.api.analyse.dto.response.DependencyResult;
import io.depsight.api.analyse.dto.response.VulnerabilityResult;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalysisOrchestrator {

    private final JarSizeFetcher jarSizeFetcher;
    private final VulnerabilityFetcher vulnerabilityFetcher;

    public Mono<AnalysisResult> enrichTree(List<DependencyNode> tree) {
        // Flatten the tree once
        List<DependencyNode> flattenedList = flatten(tree);

        // 2. Fetch sizes and vulnerabilities concurrently
        Mono<Map<String, Long>> sizeMono = jarSizeFetcher.fetchJarSizes(flattenedList);
        Mono<Map<String, List<VulnerabilityResult>>> vulnsMono =
                vulnerabilityFetcher.fetchVulnerabilities(flattenedList);

        // 3 Combine the results reactively
        return Mono.zip(sizeMono, vulnsMono).map(tuple -> {
            Map<String, Long> sizes = tuple.getT1();
            Map<String, List<VulnerabilityResult>> vulns = tuple.getT2();

            Long totalSizeBytes = sizes.values().stream().reduce(0L, Long::sum);

            List<DependencyResult> dependencyResults =
                    tree.stream().map(node -> transformNode(node, sizes, vulns)).toList();

            return new AnalysisResult(dependencyResults, totalSizeBytes, formatJarSize(totalSizeBytes));
        });
    }

    public DependencyResult transformNode(
            DependencyNode node, Map<String, Long> sizes, Map<String, List<VulnerabilityResult>> vulns) {
        String key = coordinateKey(node);
        Long sizeBytes = sizes.get(key);
        String humanReadableSize = (sizeBytes == null) ? "Unknown" : formatJarSize(sizeBytes);

        List<VulnerabilityResult> nodeVulns = vulns.getOrDefault(key, List.of());
        List<DependencyResult> children = node.children().stream()
                .map(child -> transformNode(child, sizes, vulns))
                .toList();

        return new DependencyResult(
                node.groupId(),
                node.artifactId(),
                node.version(),
                node.scope(),
                node.depth(),
                sizeBytes,
                humanReadableSize,
                nodeVulns,
                children);
    }

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
    private void flattenNode(DependencyNode node, Set<String> visited, List<DependencyNode> flattenedList) {
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

    private String formatJarSize(Long sizeInBytes) {
        if (sizeInBytes == 0) return "0B";
        if (sizeInBytes < 0) {
            log.warn("Bytes less than 0");
            return "Invalid size";
        }
        final String[] units = {"B", "KB", "MB", "GB", "TB", "PB", "EB"};

        int unitIndex = (int) (Math.log(sizeInBytes) / Math.log(1024));

        if (unitIndex >= units.length) {
            unitIndex = units.length - 1;
        }

        double size = sizeInBytes / Math.pow(1024, unitIndex);

        return String.format("%.2f %s", size, units[unitIndex]);
    }

    private String coordinateKey(DependencyNode node) {
        return node.groupId() + ":" + node.artifactId() + ":" + node.version();
    }
}
