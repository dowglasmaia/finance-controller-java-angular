# Testing and Observability

Choose verification from the risk. Keep domain unit tests for domain rules; use PostgreSQL Testcontainers for SQL, mappings, constraints, transactions, migrations, locks, and engine-specific behavior. Do not introduce H2 in this project.

Useful integration evidence includes:

- Repository result correctness and `owner_id` isolation for every tenant-owned operation.
- Query count or Hibernate statistics for suspected N+1 regressions, paired with result-shape and row-volume checks.
- Entity graph initialization, pagination ordering, duplicate roots, and boundary cases.
- Commit/rollback behavior, optimistic conflict outcomes, uniqueness races, and bounded retry behavior.
- Flyway migration from empty schema and supported prior versions, including constraints and representative data.

Keep query-count assertions focused and stable; avoid brittle full SQL-string snapshots. Performance thresholds need a representative baseline and controlled environment. Use JMH for JVM-only microbenchmarks, not database round trips. Use load/integration tests for database latency and concurrency.

Observability should capture safe aggregates: query duration/count, pool wait/active connections, failures, and migration outcome. Redact bind parameters that may contain personal/financial data; never log secrets or production records. Distinguish sampled production evidence from test results and disclose missing telemetry.
