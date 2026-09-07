package io.depsight.api.analyse.resolver;

import io.depsight.api.analyse.dto.response.ConflictResult;
import io.depsight.api.analyse.resolver.dto.VersionRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConflictDetector {

    public List<ConflictResult> detectConflicts(Map<String, List<VersionRequest>> versionCollector) {
        if (versionCollector.isEmpty()) {
            log.info("versionCollector map is empty");
            return List.of();
        }

        List<ConflictResult> conflicts = new ArrayList<>();
        for (Map.Entry<String, List<VersionRequest>> entry : versionCollector.entrySet()) {
            String key = entry.getKey();
            List<VersionRequest> requests = entry.getValue();

            Set<String> distinctVersions = new HashSet<>();
            for (VersionRequest request : requests) {
                distinctVersions.add(request.version());
            }

            if (distinctVersions.size() <= 1) {
                continue;
            }

            // Nearest wins (lowest depth). Stable sort preserves declaration order for ties.
            List<VersionRequest> sorted = new ArrayList<>(requests);
            sorted.sort(Comparator.comparingInt(VersionRequest::depth));

            String resolvedVersion = sorted.getFirst().version();
            conflicts.add(new ConflictResult(key, resolvedVersion, requests));
        }

        return conflicts;
    }
}
