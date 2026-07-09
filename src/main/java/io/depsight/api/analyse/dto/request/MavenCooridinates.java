package io.depsight.api.analyse.dto.request;

/**
 * Carries one parent BOM location
 *
 * @param parentGroupId
 * @param parentArtifactId
 * @param parentVersion
 */
public record MavenCooridinates(
    String parentGroupId, String parentArtifactId, String parentVersion) {}
