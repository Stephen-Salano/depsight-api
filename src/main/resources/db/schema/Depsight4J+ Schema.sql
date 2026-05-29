CREATE TYPE "scope" AS ENUM (
  'COMPILE',
  'PROVIDED',
  'RUNTIME',
  'TEST',
  'SYSTEM',
  'IMPORT'
);

CREATE TYPE "analysis_status" AS ENUM (
  'PENDING',
  'PROCESSING',
  'COMPLETED',
  'FAILED'
);

CREATE TABLE dependencies (
  "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
  "name" text NOT NULL,
  "group_id" varchar NOT NULL,
  "artifact_id" varchar NOT NULL,
  "version" varchar NOT NULL,
  "scope" scope DEFAULT 'COMPILE',
  "packaging" varchar DEFAULT 'jar',
  "classifier" varchar,
  "optional" bool DEFAULT false,
  "system_path" varchar,
  "artifact_size" bigint,
  "analysis_id" uuid NOT NULL,
  "transitive_count" int,
  "latest_version" varchar,
  "is_outdated" boolean DEFAULT false
);

CREATE TABLE dependency_links (
  "id" UUID PRIMARY KEY DEFAULT (gen_random_uuid()),
  "parent_id" UUID NOT NULL,
  "child_id" UUID NOT NULL,
  "analysis_id" uuid NOT NULL
);

CREATE TABLE vulnerabilities (
  "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
  "dependency_id" uuid NOT NULL,
  "vuln_id" varchar UNIQUE NOT NULL,
  "cve_id" varchar,
  "aliases" text[],
  "summary" text,
  "cwes" text[],
  "cvss_severity" float,
  "cvss_vector" varchar,
  "epss" float,
  "is_malware" boolean DEFAULT false,
  "kev" boolean DEFAULT false,
  "affected_ecosystems" text[],
  "source" varchar,
  "published_at" timestamp,
  "references_url" jsonb
);

CREATE TABLE analyses (
  "id" uuid PRIMARY KEY DEFAULT (gen_random_uuid()),
  "project_name" varchar,
  "group_id" varchar,
  "artifact_id" varchar,
  "version" varchar,
  "status" analysis_status NOT NULL DEFAULT 'PENDING',
  "total_dependencies" int DEFAULT 0,
  "total_vulnerabilities" int DEFAULT 0,
  "has_conflicts" boolean DEFAULT false,
  "created_at" timestamp NOT NULL DEFAULT (now())
);

CREATE UNIQUE INDEX "unique_maven_artifact" ON dependencies ("analysis_id", "group_id", "artifact_id", "version");

CREATE UNIQUE INDEX ON dependency_links ("parent_id", "child_id");

CREATE UNIQUE INDEX ON vulnerabilities ("dependency_id", "vuln_id");

COMMENT ON TABLE dependencies IS 'Main dependency table where a single dependency (including transitives) are stored';

COMMENT ON COLUMN dependencies."name" IS 'Human-readable name (often pulled from the library''s POM file).';

COMMENT ON COLUMN dependencies."group_id" IS 'The project''s background/organization (e.g., org.springdoc).';

COMMENT ON COLUMN dependencies."artifact_id" IS 'The name of the jar/project (e.g., springdoc-openapi...).';

COMMENT ON COLUMN dependencies."version" IS 'The specific version (e.g., 3.0.2).';

COMMENT ON COLUMN dependencies."packaging" IS 'It defines the artifacts file extension (e.g., jar, war, pom, aar). The default is jar. Can also be custom plugin types';

COMMENT ON COLUMN dependencies."classifier" IS 'Used to distinguish artifacts built from the same POM but with different content (e.g., sources, javadoc, linux-x86_64).';

COMMENT ON COLUMN dependencies."system_path" IS 'Required only if scope is system.';

COMMENT ON COLUMN dependencies."artifact_size" IS 'JAR/artifact sizes are typically stored in bytes as whole numbers (e.g. 3145728 for 3MB).';

COMMENT ON COLUMN dependencies."transitive_count" IS 'Number of transitive deps found';

COMMENT ON TABLE dependency_links IS 'separate table whose only job is to link two dependencies together';

COMMENT ON TABLE vulnerabilities IS 'CVE/dependency known/reported vulnerability';

COMMENT ON COLUMN vulnerabilities."vuln_id" IS 'Sonatype ID e.g. sonatype-2026-003445';

COMMENT ON COLUMN vulnerabilities."cve_id" IS 'Extracted from aliases, nullable';

COMMENT ON COLUMN vulnerabilities."aliases" IS 'All known identifiers e.g. CVE, GHSA';

COMMENT ON COLUMN vulnerabilities."summary" IS 'Plain English description of the vulnerability';

COMMENT ON COLUMN vulnerabilities."cwes" IS 'Weakness categories e.g. CWE-89';

COMMENT ON COLUMN vulnerabilities."cvss_severity" IS 'Numeric score 0-10';

COMMENT ON COLUMN vulnerabilities."cvss_vector" IS 'e.g. CVSS:3.1/AV:L/AC:L/...';

COMMENT ON COLUMN vulnerabilities."epss" IS 'Exploit probability 0-1';

COMMENT ON COLUMN vulnerabilities."kev" IS 'On CISA Known Exploited vulnerabilities list';

COMMENT ON COLUMN vulnerabilities."affected_ecosystems" IS 'e.g. Maven, npm';

COMMENT ON COLUMN vulnerabilities."source" IS 'e.g. Sonatype, NVD';

COMMENT ON COLUMN vulnerabilities."references" IS 'Array of advisory/patch links';

COMMENT ON TABLE analyses IS 'One row per pom.xml upload and analysis run';

COMMENT ON COLUMN analyses."project_name" IS 'From <name> in pom.xml';

COMMENT ON COLUMN analyses."group_id" IS 'From <groupId>';

COMMENT ON COLUMN analyses."artifact_id" IS 'From <artifactId>';

COMMENT ON COLUMN analyses."version" IS 'From <version>';

ALTER TABLE dependencies ADD FOREIGN KEY ("analysis_id") REFERENCES analyses ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE dependency_links ADD FOREIGN KEY ("parent_id") REFERENCES dependencies ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE dependency_links ADD FOREIGN KEY ("child_id") REFERENCES dependencies ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE dependency_links ADD FOREIGN KEY ("analysis_id") REFERENCES analyses ("id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE vulnerabilities ADD FOREIGN KEY ("dependency_id") REFERENCES dependencies ("id") DEFERRABLE INITIALLY IMMEDIATE;
