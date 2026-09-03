# AGENTS.md

## Build & Test

```sh
./mvnw compile          # compile
./mvnw test             # run all tests (H2 in-memory, no external deps)
./mvnw package -DskipTests  # build JAR
docker compose up --build   # run full stack (app + PostgreSQL)
```

No separate lint/typecheck steps. No CI config exists.

## Stack

- **Spring Boot 4.1.1** / **Java 25** — not 3.x. Verify `--release 25` in Maven config.
- **Jackson 3.x** — namespace is `tools.jackson.*`, NOT `com.fasterxml.jackson.*`. This is the biggest gotcha if copy-pasting from older examples.
- **PostgreSQL** in production, **H2** in tests. DB connection configured via `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` env vars.
- **`import.sql`** runs on every startup (`spring.sql.init.mode=always`) and seeds 3 templates. DDL mode is `create` (tables are dropped/recreated each start).

## Code Conventions

- Package: `com.adobe.printservice`
- REST controllers named `*Resource` (not `*Controller`). See `web/RenderTemplateResource.java`.
- Constructor injection only — no `@Autowired` on fields.
- No Lombok. Manual getters/setters on all entities.
- JPA entities use `String` IDs (UUID strings), not `@GeneratedValue`.
- JSON stored as `TEXT` column via `JsonMapConverter` (JPA `AttributeConverter`).
- Tests use `@SpringBootTest` + `@AutoConfigureMockMvc` (imported from `org.springframework.boot.webmvc.test.autoconfigure`). MockMvc assertions via `jsonPath()`.

## What's Implemented vs. Not

**Given (do not modify):** `Job` entity, `JobStatus` enum, `RenderTemplate` entity, `JsonMapConverter`, `RenderTemplateRepository`, `RenderTemplateResource` (GET /templates), seed data, Dockerfile.

**Must be built:** `JobRepository`, job REST endpoints (POST/GET /jobs, GET /jobs/{id}/result), async processor, retry logic, `docker-compose.yml`, liveness/readiness endpoints, metrics endpoint.

## Docker

- `Dockerfile` is provided (multi-stage: `eclipse-temurin:25-jdk` build → `eclipse-temurin:25-jre` run).
- `docker-compose.yml` does not exist yet — you must create it. Wire the app service with a `postgres` container. App already reads DB config from env vars.
- H2 must NOT be used in the running application — only in tests.

## Template IDs (seeded)

| ID | Name |
|----|------|
| `b6f1e6a2-6b8b-4a9d-9c2e-3f2d8a2f9b10` | invoice-standard |
| `6c6a1a44-4f0b-4a8a-8b8e-2b1e9c9c2a11` | shipping-label |
| `9e2b6f2a-2d8a-4b1a-9f3d-7a1c5e6b8c12` | certificate-of-completion |
