# ADR 0003: Elasticsearch as a derived search projection

- **Status:** Accepted
- **Date:** 2026-10-16

## Context

Analysts need to filter and paginate solicitations by status, service type, priority, state, and date. These read patterns differ from the transactional workload. Authorization by analyst coverage must still be enforced regardless of search filters.

## Decision

Use Elasticsearch only as a derived read projection. PostgreSQL remains authoritative. Documents are rebuildable and indexed by solicitation ID so processing the same update is idempotent. Analyst coverage is intersected with all requested filters and is never delegated to the search engine as the sole authorization barrier.

Projection updates are delivered through the transactional outbox described in [ADR 0004](0004-transactional-outbox.md).

## Consequences

- Search can be unavailable without blocking business writes.
- Search results are eventually consistent; reads by ID continue to use PostgreSQL.
- The index can be rebuilt from authoritative data and events.
- Elasticsearch adds infrastructure and operational work.

## Alternatives considered

- **PostgreSQL for all search:** rejected for the expected text and faceted query workload.
- **Dual-write PostgreSQL and Elasticsearch:** rejected because partial failure can leave the stores inconsistent.
- **Elasticsearch as the source of truth:** rejected because it does not provide the transactional guarantees required by the domain.
