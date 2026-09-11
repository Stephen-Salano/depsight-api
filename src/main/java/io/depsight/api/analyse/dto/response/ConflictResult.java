package io.depsight.api.analyse.dto.response;

import io.depsight.api.analyse.resolver.dto.VersionRequest;
import java.util.List;

/**
 *  <p>represents a version conflict resolution.</p>
 *  It has a:
 *   @param key identifying which dependency conflicted,
 *   @param resolvedVersion that was chosen,
 *   @param requests list showing what versions were requested and by whom.
 */
public record ConflictResult(String key, String resolvedVersion, List<VersionRequest> requests) {
    public ConflictResult {
        requests = List.copyOf(requests);
    }

    public record ConflictingVersion(String version, int depth) {}
}
