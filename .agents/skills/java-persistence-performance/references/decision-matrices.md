# Persistence Decision Matrices

## Read-path shape

| Situation | Start with | Check before adopting |
|---|---|---|
| API needs a small fixed view | DTO/projection query | Contract semantics, joins, and query plan |
| Need entities with selected relations | Entity graph or explicit fetch plan | Row multiplication, lazy access, and pagination |
| N+1 over many roots | Query count and access trace; then graph, projection, batch, or second query | Rows/bytes, memory, collection-fetch pagination |
| Deep sequential pages | Keyset pagination | Stable unique cursor and changed navigation behavior |

## Write and consistency

| Invariant | Candidate mechanism | Main caveat |
|---|---|---|
| Conflicting update to one row | Optimistic `@Version` | Caller must handle conflict; cross-row rules remain |
| Serialize a contested row | Pessimistic lock | Contention, deadlock, and short transaction required |
| Uniqueness under racing inserts | PostgreSQL unique constraint | Translate violation to approved application behavior |
| Atomic balance-like update | Conditional SQL update plus affected-row check | Predicate must encode the full approved invariant |
| Cross-row predicate under races | Constraint, full locking, or suitable isolation | Confirm PostgreSQL semantics and retry safety |

## Schema rollout

| Change | Safer starting approach | Evidence needed |
|---|---|---|
| Add nullable field/table | Expand migration | Compatibility with old and new app versions |
| Add required field to populated table | Expand, backfill, validate, enforce | Duration, defaults, null rows, write races |
| Add large-table index | Evaluate concurrent build | Flyway transaction mode, lock/IO impact, plan use |
| Rename/remove field | Expand and contract across deployments | No old reader/writer remains; recovery plan |
| Checksum mismatch | Inspect history and deployed state | Never repair/edit automatically |

These are starting points, not automatic prescriptions. Confirm product semantics, PostgreSQL version, data distribution, workload, and deployment process.
