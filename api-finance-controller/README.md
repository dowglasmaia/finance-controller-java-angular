# api-finance-controller

Java 25, Spring Boot 4.1.1, Spring Modulith 2.1.1.

## Run

Requires JDK 25 or newer. Maven Wrapper downloads Maven 3.9.16 on first use (internet required). On Windows use `mvnw.cmd` in place of `./mvnw`.

PostgreSQL 18.1 is required (Docker). Copy `.env.example` to `.env` (ignored by Git), then start the database with `docker compose up -d db` **from the repository root** (the `compose.yml` lives there and also builds the API and frontend images; see the root README and `scripts/`); the `db-data` volume keeps data between restarts. Export the datasource variables before running the app (`SPRING_DATASOURCE_PASSWORD` must match `POSTGRES_PASSWORD`; URL and user default to the compose values in the `local` profile). Flyway applies `db/migration` at startup and JPA only validates the schema (`ddl-auto=validate`). Integration tests start their own PostgreSQL through Testcontainers, so Docker must be running for `./mvnw clean verify`.

```sh
# from the repository root: docker compose up -d db   (or .\scripts\up-back.ps1 -Local)
export SPRING_DATASOURCE_PASSWORD=<same as POSTGRES_PASSWORD>   # PowerShell: $env:SPRING_DATASOURCE_PASSWORD="..."
./mvnw clean verify
./mvnw spring-boot:run
```

Authentication: `APP_JWT_SECRET` (at least 32 bytes) is required outside the `local` and `test` profiles and the app refuses to start without it; `.env.example` and the `local` profile carry a fictitious development value that must never be used in production. Set `APP_CORS_ALLOWED_ORIGINS` to the frontend origin. Every route except `POST /api/v1/auth/{register,login,refresh,logout}` and `GET /actuator/health` requires `Authorization: Bearer <accessToken>`, so the endpoints below return 401 without a token.

Then register through `POST /api/v1/auth/register` and call `GET http://localhost:8080/api/v1/categories` with the returned token to see the 11 default categories, and `GET http://localhost:8080/actuator/health` for an UP status. Set `PORT` to change the port.

## Architecture

`identity` and `category` are Spring Modulith modules. HTTP API → use case (one concrete class per use case) → output port → JPA adapter. The immutable domain records have no framework dependencies. Modules talk through their root package only (`CurrentOwner`, `UserRegistered`, `CategoryUsageGuard`); add business features as sibling root packages and keep internal types private. `ModularityTests` verifies boundaries.

Run `node <skill>/scripts/validate.mjs .` for build, startup and HTTP checks. Logs are stored in a temporary validation folder and reported on failure.


## Contract First
Each module owns a contract in `src/main/resources/openapi/modules/<module>.yaml` (shared errors in `openapi/common`). Edit it, then run `./mvnw clean verify`. Maven generates API interfaces and transport models under `target/generated-sources/openapi/<module>/src/main/java`. Implement the generated interface in the module adapter and map transport models to domain models. Never edit generated files.

## Logging
Logs are structured JSON on stdout (Spring Boot native structured logging over Logback, ECS format) in every profile except `local`, which prints readable text. Choose the format with `LOG_FORMAT=ecs|logstash|gelf` and levels with `LOG_LEVEL_ROOT` / `LOG_LEVEL_APP`. `RequestLoggingFilter` (`shared/logging`) accepts or creates `X-Request-Id`, returns it in the response, adds `requestId` to every log line and writes one `request completed` line per request (method, path, status, duration; `/actuator` excluded). It never logs query strings, headers or bodies. Never log passwords, tokens, personal or financial data. Use SLF4J with `logger.atInfo().addKeyValue("key", value).log("message")` for extra fields.

## Observability
Tracing and metrics use Spring Boot's native OpenTelemetry support. `traceId`/`spanId` appear in every log line. OTLP export is off by default; set `OTEL_EXPORT_ENABLED=true` and the standard `OTEL_EXPORTER_OTLP_ENDPOINT` to send traces and metrics to an OpenTelemetry Collector. Sampling: `TRACING_SAMPLING_PROBABILITY`. Do not put personal or financial data in span attributes.
