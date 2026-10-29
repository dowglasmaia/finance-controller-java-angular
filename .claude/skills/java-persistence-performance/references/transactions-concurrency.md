# Transactions and Concurrency

## Transaction boundaries

Place the transaction around the application operation that must be atomic, following the existing use-case/port architecture. Confirm that the call crosses the Spring proxy; self-invocation does not trigger proxy advice. Review propagation, isolation, read-only intent, timeout, rollback rules (including checked exceptions), flush/commit timing, and lazy access after the boundary.

Keep transactions short. Do not hold locks while calling remote services or doing slow non-database work. Open Session in View can hide query behavior beyond the service boundary; inspect the actual configuration before relying on it. Database constraints remain the final protection for uniqueness and referential invariants.

## Concurrency diagnosis

Write down the competing transactions and a possible interleaving. Identify the invariant and whether it spans one row, several rows, or absence/uniqueness. Then select and verify a mechanism:

- `@Version` optimistic locking for conflicting updates to a versioned row; surface conflict semantics deliberately.
- Pessimistic row locks only when conflicts and latency justify serialization; analyze lock ordering, timeout, deadlock, and transaction duration.
- Atomic conditional SQL or unique/check constraints for invariants PostgreSQL can enforce directly; check affected-row counts and constraint translation.
- Serializable or stronger isolation only when needed, with awareness of PostgreSQL serialization failures and bounded, safe retries.

Optimistic locking on one entity does not automatically protect cross-row invariants. A read-then-write check can race; move the invariant into a constraint, lock the full relevant set, or use a suitable atomic operation. Retries require a bounded policy and repeat-safe operation; never retry arbitrary integrity or programming failures.

## Verification

Use coordinated transactions/connections in PostgreSQL integration tests to reproduce the race, assert the allowed outcome, and prove tenant boundaries. Avoid tests that pass merely because execution happened sequentially. For external effects, consider the consistency boundary and idempotency rather than assuming the database transaction covers them.
