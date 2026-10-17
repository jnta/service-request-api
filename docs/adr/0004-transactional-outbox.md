# ADR 0004: Transactional outbox

- **Status:** Accepted and implemented
- **Date:** 2026-10-16

## Context

Solicitation changes must eventually update the Elasticsearch projection. PostgreSQL and Elasticsearch do not share a transaction. A direct dual write can commit one side and fail on the other, while an in-memory retry does not survive a process restart.

## Decision

Persist an outbox event in the same PostgreSQL transaction as each relevant domain change. A background processor reads pending events, updates Elasticsearch, and marks events as processed. Indexing by solicitation ID makes retries idempotent. Failed events remain pending for retry.

The search projection is indexed by the processor after the database transaction commits.

## Consequences

- Domain changes and the intent to update search commit together.
- Elasticsearch outages do not fail business transactions.
- Projection updates are eventually consistent and require monitoring and retry handling.
- The processor must handle duplicate delivery and preserve event ordering where required.
- The application gains an outbox table and a background processor.

## Alternatives considered

- **In-memory retries:** rejected because pending work is lost on process failure.
- **Distributed transactions:** rejected because Elasticsearch does not provide a suitable transactional participant.
- **CDC with Debezium:** a valid option, but Kafka and Connect add disproportionate infrastructure for this project.
- **Periodic batch rebuild only:** rejected as the primary update mechanism because the consistency window would be too large.
