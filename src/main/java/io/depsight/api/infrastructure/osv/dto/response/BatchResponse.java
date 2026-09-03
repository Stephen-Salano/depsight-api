package io.depsight.api.infrastructure.osv.dto.response;

import java.util.List;

public record BatchResponse(List<BatchResult> results) {}
