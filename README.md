# DepSight4J+

Java dependency analysis tool — paste your `pom.xml` and instantly see your
full dependency graph, JAR sizes, CVEs, and version conflicts.

## Status
Phase 1 complete (M3–M7)

## Tech Stack
- Java 21 + Spring Boot 4.0.6
- PostgreSQL + Flyway
- Maven Central API + OSV API

## Features (Phase 1)
- pom.xml upload and parsing
- Full transitive dependency resolution via BFS (configurable depth, max 6)
- Parent BOM import resolution
- JAR size estimation via Maven Central HEAD requests
- CVE/vulnerability scanning via OSV (osv.dev) with batch queries
- Severity mapping (LOW / MEDIUM / HIGH / CRITICAL)
- Request-level timeout and source-aware error handling

### Planned
- Version conflict detection
- Single dependency search
- Platform analytics dashboard

## Running Locally

### Prerequisites
- Java 21+
- Maven 3.9+ (or use the included `mvnw` wrapper)
- PostgreSQL 15+

### Database setup
```sql
CREATE DATABASE depsight4j;
```
Flyway runs automatically on startup and applies all migrations.

### Environment variables
| Variable | Default | Description |
|----------|---------|-------------|
| `DEV_DB_URL` | `jdbc:postgresql://localhost:5432/depsight4j` | JDBC URL |
| `DEV_DB_USERNAME` | `postgres` | DB username |
| `DEV_DB_PASSWORD` | — | DB password |
| `FRONTEND_URL` | `http://localhost:3000` | Allowed CORS origin |

### Run
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

The API starts on **http://localhost:8080**.
Swagger UI is available at **http://localhost:8080/swagger-ui/index.html**.
Actuator health check runs on port **9091** (`/actuator/health`).

## Architecture
_Coming soon_
