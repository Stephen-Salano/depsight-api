package io.depsight.api.analyse.dto.request;

public record ParsedDependency(String groupId, String artifactId, String version, String scope) {}
