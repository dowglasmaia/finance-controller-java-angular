# JPA and Hibernate Performance

## Inspect before changing

Trace the operation from use case through the persistence port and adapter. Inspect entity ownership, relationship cardinality, fetch plans, query, transaction, and generated SQL. Confirm the resolved Hibernate major version before relying on provider-specific settings or APIs. Distinguish Jakarta Persistence guarantees from Hibernate behavior and PostgreSQL behavior.

## Fetching and N+1

- Treat mapping fetch type as a default, not a complete per-operation fetch plan. Prefer operation-specific entity graphs, DTO/projection queries, or explicit follow-up queries when they fit the use case.
- Diagnose N+1 with SQL logs/statistics or a query-count integration test over representative results; include nested relationships and empty/singleton cases where relevant.
- A fetch join can reduce round trips but multiply rows, duplicate roots, load excess columns, or create a Cartesian product. Avoid fetching multiple collections in one query without validating provider support and result shape.
- Collection fetch joins and pagination do not compose safely in many ORM configurations. Page root identifiers first and load the needed graph separately, or use a projection/keyset design; verify ordering and duplicates.
- Batch fetching can reduce selects while retaining entity semantics. Choose batch size from evidence and account for parameter limits and memory.
- JDBC batching primarily helps repeated writes; it is not the same as batch fetching. Check identifier generation, flush/clear cadence, statement ordering, and driver/provider support.

## Persistence-context costs

Understand entity states, `persist` versus `merge`, dirty checking, flush timing, cascade/orphan behavior, and first-level cache. Large units of work retain managed objects; use bounded chunks and deliberate flush/clear only when semantics allow. Bulk JPQL/SQL bypasses normal entity synchronization and callbacks; clear or refresh affected state and preserve tenant predicates.

Use read-only transactions/hints only when they match the operation and confirmed stack behavior. Do not enable second-level or query caching as a generic performance fix; define invalidation, staleness, memory, and tenant key behavior first. Avoid entity `equals`/`hashCode` schemes that change after persistence or traverse lazy relationships.

## Evidence for improvement

Compare statement count, returned rows, transferred columns/bytes where available, execution time, allocation/memory, and query plan under a representative workload. Record the data shape and environment. A lower query count can still be slower or more memory-intensive.
