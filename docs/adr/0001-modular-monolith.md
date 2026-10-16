# ADR 0001: Modular monolith

- **Status:** Accepted
- **Date:** 2026-10-16

## Context

The system has distinct capabilities—identity, solicitations, search, and audit—but is being built by a small team for a technical challenge. Independent services would add deployment, service discovery, distributed tracing, and cross-service consistency concerns without solving a current scaling need.

## Decision

Build a modular monolith: one deployable application organized by business capability. Modules own their state and communicate through explicit interfaces. The shared module may be depended on by feature modules but must not depend on them. The solicitation domain remains independent of Spring, persistence, and HTTP.

## Consequences

- One deployment and local Compose stack keep operations straightforward.
- Local ACID transactions support the transactional outbox.
- Clear module boundaries make future extraction possible.
- Package boundaries are conventions unless enforced. Add architecture tests when the modules and internal layers are established.
- Scaling applies to the whole application rather than to individual capabilities.

## Alternatives considered

- **Microservices:** rejected because their operational and consistency costs are disproportionate at this stage.
- **Top-level technical layers:** rejected because each capability would be spread across unrelated packages.
- **JPMS modules:** rejected because Spring's reflection and classpath conventions make the added friction unjustified here.
