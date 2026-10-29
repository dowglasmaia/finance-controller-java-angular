# SQL, Indexing, and PostgreSQL Plans

Start from the SQL actually executed, its parameters, realistic cardinalities, and PostgreSQL `EXPLAIN (ANALYZE, BUFFERS)` when execution is safe and representative. `ANALYZE` runs the statement; use a read-only transaction or a non-mutating query and understand the cost before using it. Plans are data- and statistics-dependent.

## Index decisions

- Derive candidate indexes from filters, joins, ordering, uniqueness, and observed plans. For multicolumn B-tree indexes, account for leftmost-prefix use and the actual predicate/order pattern.
- Check selectivity, row estimates, table size, write frequency, existing indexes, index-only opportunities, and storage/write amplification. A WHERE clause alone is not evidence for an index.
- PostgreSQL partial, expression, and covering indexes are workload-specific tools; verify planner use and maintenance cost. Confirm concurrent index creation constraints for migrations.
- If an index is ignored, inspect selectivity, casts/types, expression mismatch, stale statistics, estimated versus actual rows, and whether a sequential scan is cheaper.

## Query shape and pagination

Compare expensive single queries with multiple focused queries. Inspect joins, sort/hash spills, rows removed, buffers, and actual/estimated cardinality. Parameterize values and avoid giant `IN` lists; consider chunking or a relational input strategy where necessary.

Offset pagination cost grows with skipped rows and requires stable ordering. Keyset pagination can reduce deep-page work but needs a unique deterministic cursor and changes navigation semantics. For either approach, preserve tenant filters and test ties, boundaries, duplicates, and concurrent inserts/deletes.

Database plans do not capture application-side hydration cost, network volume, or connection-pool wait. Correlate plan evidence with service metrics, result sizes, and pool saturation before concluding.
