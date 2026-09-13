package io.depsight.api.analyse.dto.response;

import java.util.List;

public record AnalysisResult(
        List<DependencyResult> dependencies,
        Long totalSizeBytes,
        String totalSize,
        boolean hasConflicts,
        List<ConflictResult> conflicts) {

    public AnalysisResult {
        // never null list of conflicts
        if (conflicts == null) {
            conflicts = List.of();
        }
        hasConflicts = !conflicts.isEmpty();
    }
}
