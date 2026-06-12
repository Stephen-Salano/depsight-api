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

