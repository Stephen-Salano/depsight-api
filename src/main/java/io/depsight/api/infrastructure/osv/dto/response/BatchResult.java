package io.depsight.api.infrastructure.osv.dto.response;

import java.util.List;

public record BatchResult(List<VulnerabilityRef> vulns) {

    public BatchResult {
        if (vulns == null) {
            vulns = List.of();
        }
    }
}
