package io.depsight.api.analyse.resolver.dto;

public record VersionRequest(String groupId, String artifactId, String version, int depth) {}
