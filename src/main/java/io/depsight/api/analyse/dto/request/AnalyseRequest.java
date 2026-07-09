package io.depsight.api.analyse.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Carry Data from the HTTP request
 *
 * @param pomXml the user pasted/attatched pom.xml file or string
 */
public record AnalyseRequest(@NotBlank String pomXml) {}
