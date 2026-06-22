package io.depsight.api.analyse.dto.request;

public record MavenCooridinates(
    String parentGroupId, String parentArtifactId, String parentVersion) {}
