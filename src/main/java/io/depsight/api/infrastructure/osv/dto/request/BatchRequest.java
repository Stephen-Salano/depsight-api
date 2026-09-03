package io.depsight.api.infrastructure.osv.dto.request;

import java.util.List;

public record BatchRequest(List<BatchQuery> queries) {}
