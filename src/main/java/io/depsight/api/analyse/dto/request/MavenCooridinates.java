package io.depsight.api.analyse.dto.request;

/** Represents a Pom that needs to be fetched */
public record MavenCooridinates(
    String parentGroupId, String parentArtifactId, String parentVersion) {}
