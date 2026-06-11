package io.depsight.api.analyse.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AnalyseRequest(@NotBlank String pomXml) {}
