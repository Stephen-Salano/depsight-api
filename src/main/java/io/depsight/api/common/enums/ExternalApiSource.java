package io.depsight.api.common.enums;

public enum ExternalApiSource {
  MAVEN_CENTRAL,
  OSV_DEV,
  DEP_DEV;

  public String toErrorCode() {
    return name() + "_ERROR";
  }
}
