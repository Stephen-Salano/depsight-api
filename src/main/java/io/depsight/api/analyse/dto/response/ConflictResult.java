package io.depsight.api.analyse.dto.response;

import io.depsight.api.analyse.resolver.dto.VersionRequest;
import java.util.List;

public record ConflictResult(String key, String resolvedVersion, List<VersionRequest> requests) {
    public ConflictResult {
        requests = List.copyOf(requests);
    }
}
