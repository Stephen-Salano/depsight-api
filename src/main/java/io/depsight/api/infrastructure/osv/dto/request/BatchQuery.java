package io.depsight.api.infrastructure.osv.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BatchQuery(@JsonProperty("package") OsvPackage osvPackage, String version) {}
