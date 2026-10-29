# Flyway and Schema Evolution

Treat versioned migrations as immutable production artifacts. Follow the repository's existing `V...__description.sql` naming and ordering. Inspect Flyway configuration, schema history, grants, and supported deployment versions before editing.

## Design and compatibility

- Prefer explicit PostgreSQL DDL and constraints that match approved behavior. Do not rely on Hibernate `update`; use controlled schema validation where configured.
- Consider application versions that may run concurrently during rollout. For breaking changes, use expand, compatible deployment, backfill, switch, then contract only after old versions are gone.
- For a populated table, assess locks, table rewrite, default behavior, row count, WAL, replication, and rollback/recovery. Backfill large datasets in bounded resumable batches where appropriate.
- Index creation can block writes. Evaluate PostgreSQL `CREATE INDEX CONCURRENTLY` and Flyway transaction behavior for the actual version; this statement cannot run inside a transaction block. Keep such operational requirements explicit and test them.
- Use `NOT NULL`, foreign keys, unique constraints, and checks only with a safe rollout for existing rows and active writers.

## Failure and recovery

On checksum or validation failure, inspect the exact migration, schema history, deployment state, and whether it was applied anywhere. Do not automatically run `repair`, edit an applied migration, or delete schema history. Correct forward with a new migration when appropriate; any repair of history needs an explicit, reviewed recovery plan.

Never assume DDL is transactionally reversible or that Flyway can undo it. Identify data loss, dependent objects, backup/restore path, and application compatibility before destructive changes. Separate local/test/staging/production implications and do not apply a production migration without explicit authorization.

## Verification

Run migrations from a clean PostgreSQL database and upgrade from each supported previous schema state. Verify constraints and representative data. Test failure behavior and the actual transaction mode for special PostgreSQL DDL. Migration success alone does not prove application compatibility.
