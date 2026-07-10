# Changelog

All notable changes to DepSight4J+ will be documented here.

Format based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

### Added

- M3: PomParser extracts direct dependencies from pasted pom.xml
  - `cleanPom()` strips XML declaration before parsing
  - `validatePom()` rejects malformed POMs with descriptive errors
  - `extractProperties()` resolves `<properties>` block into key-value map
  - `extractDependencies()` resolves `${}` placeholders against properties map, marks unresolvable versions as `UNRESOLVED`
- `POST /api/analyse` endpoint wired through `AnalyseController` → `AnalyseService` → `PomParser`

- M4: `BfsResolver` fetches one level of transitive dependencies from Maven Central
  - `DependencyNode` record introduced as internal BFS working structure (`groupId`, `artifactId`, `version`, `scope`, `depth`, `children`)
  - Parallel fetches per level via `Flux.fromIterable` + `flatMap`, single `.collectList().block()` at the end
  - Visited set keyed on `groupId:artifactId` (no version) for cycle detection
  - Skips `UNRESOLVED` versions, `test` scope, and `provided` scope before fetching
  - Known accepted edge case: `micrometer-registry-prometheus` stays `UNRESOLVED` (version lives in an imported `micrometer-bom`, not in `spring-boot-dependencies` directly)
- M5: Full transitive tree resolves recursively up to a configurable depth
  - `BfsResolver.resolveNode()` recurses per-node (`Mono<DependencyNode>`), replacing the single-level `flatMap` pass from M4
  - `PomParser.extractImports()` detects `<dependencyManagement>` entries with `type=pom` and `scope=import`, resolving `${}` versions against properties
  - `ParentBomResolver.resolveBomImports()` fetches each import's POM and merges its `dependencyManagement` into a version map (fully reactive, no `.block()`, since it runs inside `BfsResolver`'s Netty event-loop thread)
  - Before recursing into a node's children, any `UNRESOLVED` child version is looked up against the resolved BOM-import map and fixed if found — resolves the M4 `micrometer-registry-prometheus` edge case
  - Depth is now a request parameter (`AnalyseRequest.maxDepth`, boxed `Integer` so `null` means "not provided"), defaulting to 6 and hard-capped at `MAX_ALLOWED_DEPTH = 6` via `Math.min`
  - Top-level request timeout (`.timeout(Duration.ofSeconds(15))`) added to prevent a slow/unreachable Maven Central call from hanging a request indefinitely
  - `ExternalApiException` gained a `source` field (`ExternalApiSource` enum: `MAVEN_CENTRAL`, `OSS_INDEX`, `DEP_DEV`) with `toErrorCode()` producing codes like `MAVEN_CENTRAL_ERROR`; `AnalysisTimeoutException` now extends `ExternalApiException` and reuses the same source/error-code plumbing
  - `GlobalExceptionHandler` returns `504 Gateway Timeout` for `AnalysisTimeoutException`, `502 Bad Gateway` for other `ExternalApiException`s, both source-aware

### Performance notes (deferred, not part of M5 scope)

- Concurrency limiting on `flatMap` (capping to 8 parallel fetches) was tried and reverted — this work is I/O-bound, not CPU-bound, so capping concurrency serializes otherwise-overlapping network waits and increases total latency instead of reducing load.
- No POM caching yet. The `visited` set dedupes fetches _within_ one request, but every new request starts from zero. Since a published Maven artifact's POM never changes, a `groupId:artifactId:version` → POM cache is the highest-impact next performance improvement — intentionally deferred rather than added ad hoc.
- A coarser "subtree" cache (caching a fully-resolved tree under e.g. `spring-boot-starter-web:4.0.6`) was considered as a smarter shape of the same idea — same deferral reasoning applies.
- Redis / background-sync dependency intelligence (querying an internal DB instead of Maven Central live) stays out of scope per the Phase 1 / Phase 3 boundary; revisit once real usage data shows Maven Central round-trips are the actual bottleneck.
