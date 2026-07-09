package io.depsight.api.analyse.dto.request;

/**
 * Carries the depndency information ParsedDependency
 *
 * @param groupId dependency groupId
 * @param artifactId dependency artifactId
 * @param version dependency Version
 * @param scope dependency scope
 */
public record ParsedDependency(String groupId, String artifactId, String version, String scope) {}
