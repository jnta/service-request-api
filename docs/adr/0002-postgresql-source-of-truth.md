# ADR 0002: PostgreSQL as the source of truth

- **Status:** Accepted
- **Date:** 2026-10-16

## Context

Solicitations, users, analyst coverage, outbox events, and audit records require durable transactions, constraints, and concurrency control. Search has different query needs and is handled separately in [ADR 0003](0003-elasticsearch-search-projection.md).

## Decision

PostgreSQL is the authoritative store for all business state. Flyway owns schema changes; Hibernate validates mappings and does not modify the schema. Persistence adapters map between the framework-independent domain model and JPA entities. Optimistic locking uses `@Version` where concurrent updates must be detected.

Applied migrations are immutable; corrections are made in a new migration.

## Consequences

- ACID transactions support consistent domain and outbox writes.
- Database constraints provide a final safeguard for invariants.
- Explicit domain-to-entity mapping adds code but keeps persistence details out of the domain.
- Search-specific queries are maintained in the derived Elasticsearch model.

## Alternatives considered

- **PostgreSQL alone for all search:** rejected for the expected text and faceted search workload.
- **MongoDB as the source of truth:** rejected because relational constraints and transaction semantics fit the domain better.
- **JPA annotations on domain aggregates:** rejected to keep the domain testable without persistence infrastructure.
