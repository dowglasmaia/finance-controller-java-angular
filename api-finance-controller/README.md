# api-finance-controller

Java 25, Spring Boot 4.1.1, Spring Modulith 2.1.1.

## Run

Requires JDK 25 or newer. Maven Wrapper downloads Maven 3.9.16 on first use (internet required). On Windows use `mvnw.cmd` in place of `./mvnw`.

```sh
./mvnw clean verify
./mvnw spring-boot:run
```

Then request `GET http://localhost:8080/api/v1/examples/1` for `{"id":1,"name":"Example","status":"ACTIVE"}` and `GET http://localhost:8080/actuator/health` for an UP status. Missing examples return 404. Set `PORT` to change the port.

## Architecture

`example` is a Spring Modulith module. HTTP API → use case (one concrete class per use case) → output port → in-memory adapter. The immutable domain record has no framework dependencies. Add business features as sibling root packages; keep internal types private to their module. `ModularityTests` verifies boundaries. Replace the in-memory adapter when persistence is needed.

Run `node <skill>/scripts/validate.mjs .` for build, startup and HTTP checks. Logs are stored in a temporary validation folder and reported on failure.


## Contract First
Edit `src/main/resources/openapi/openapi.yaml`, then run `./mvnw clean verify`. Maven generates API interfaces and transport models under `target/generated-sources/openapi/src/main/java`. Implement the generated interface in the module adapter and map transport models to domain models. Never edit generated files.

## Logging
Logs are structured JSON on stdout (Spring Boot native structured logging over Logback, ECS format) in every profile except `local`, which prints readable text. Choose the format with `LOG_FORMAT=ecs|logstash|gelf` and levels with `LOG_LEVEL_ROOT` / `LOG_LEVEL_APP`. `RequestLoggingFilter` (`shared/logging`) accepts or creates `X-Request-Id`, returns it in the response, adds `requestId` to every log line and writes one `request completed` line per request (method, path, status, duration; `/actuator` excluded). It never logs query strings, headers or bodies. Never log passwords, tokens, personal or financial data. Use SLF4J with `logger.atInfo().addKeyValue("key", value).log("message")` for extra fields.

## Observability
Tracing and metrics use Spring Boot's native OpenTelemetry support. `traceId`/`spanId` appear in every log line. OTLP export is off by default; set `OTEL_EXPORT_ENABLED=true` and the standard `OTEL_EXPORTER_OTLP_ENDPOINT` to send traces and metrics to an OpenTelemetry Collector. Sampling: `TRACING_SAMPLING_PROBABILITY`. Do not put personal or financial data in span attributes.
