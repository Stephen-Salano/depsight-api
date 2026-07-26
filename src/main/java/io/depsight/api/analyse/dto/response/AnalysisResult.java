package io.depsight.api.analyse.dto.response;

import java.util.List;

public record AnalysisResult(
    List<DependencyResult> dependencies, Long totalSizeBytes, String totalSize) {}
