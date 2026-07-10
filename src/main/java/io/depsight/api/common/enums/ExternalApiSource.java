package io.depsight.api.common.enums;

public enum ExternalApiSource {
  MAVEN_CENTRAL,
  OSS_INDEX,
  DEP_DEV;

  public String toErrorCode() {
    return name() + "_ERROR";
  }
}
