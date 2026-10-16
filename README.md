# Service Request API

An API for service requests with a multi-step workflow, geographic analyst authorization, Elasticsearch search, a transactional outbox, and an audit trail.

This repository follows [`docs/implementation-plan.md`](docs/implementation-plan.md). The current phase status is tracked in the [roadmap](#roadmap).

## Overview

Clients create a request and complete it in three steps: basic data, address, and confirmation. Once submitted, a request becomes read-only and enters the analyst queue. Analysts can access requests only in the states covered by their assigned regions, then start a review and approve or reject the request.

## Run the application

### Requirements

- Docker and Docker Compose
- JDK 21 for local development. The project pins Java 21 in [`.mise.toml`](.mise.toml); run `mise install` when using mise.

### Start the full stack

```bash
docker compose up --build
```

Compose starts PostgreSQL, Elasticsearch, and the application. The application waits for both dependencies to become healthy. Flyway applies pending migrations at startup.

Check application health:

```bash
curl localhost:8080/actuator/health
```

API documentation:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

Stop the services and remove their data volumes:

```bash
docker compose down -v
```

The current user must be able to access Docker. If needed, add the user to the `docker` group and start a new login session. Integration tests also require Docker access for Testcontainers.

### Run the application outside Docker

```bash
docker compose up -d postgres elasticsearch
mise exec -- ./gradlew bootRun
```

## Test

```bash
mise exec -- ./gradlew test
```

Integration tests use Testcontainers with real PostgreSQL and Elasticsearch instances. Spring Boot derives their connection details through `@ServiceConnection`; tests do not hard-code hosts, ports, or passwords.

The test strategy follows a pyramid: many domain unit tests, a smaller number of integration tests, and a few end-to-end acceptance tests. BDD scenarios are documented in [`docs/features/`](docs/features/) and implemented with JUnit; Cucumber is not used.

## Architecture

**Architecture Style: Modular Monolith**

The application is organized by business capability. Modules communicate through explicit interfaces, and the `shared` module has no dependency on feature modules. The solicitation domain remains independent of Spring, JPA, and HTTP.

### Architectural decisions

```
Transactional database: PostgreSQL
Search engine:          Elasticsearch

PostgreSQL is the source of truth.
Elasticsearch is a derived search projection.
```

The reasoning and trade-offs are documented in the [ADRs](docs/adr/):

| ADR | Decision |
| --- | --- |
| [0001](docs/adr/0001-modular-monolith.md) | Modular monolith |
| [0002](docs/adr/0002-postgresql-source-of-truth.md) | PostgreSQL as the source of truth |
| [0003](docs/adr/0003-elasticsearch-search-projection.md) | Elasticsearch as a derived search projection |
| [0004](docs/adr/0004-transactional-outbox.md) | Transactional outbox |

### Local infrastructure

| Service | Image | Port |
| --- | --- | --- |
| app | Local build, Java 21 | 8080 |
| postgres | `postgres:17-alpine` | 5432 |
| elasticsearch | `docker.elastic.co/elasticsearch/elasticsearch:9.3.3` | 9200 |

Flyway owns the database schema. Hibernate uses `ddl-auto: validate` and does not modify it. Migrations will be added with the first real application schema in the persistence phase.

## Domain and workflow

A new solicitation starts with `status = DRAFT` and `currentStep = BASIC_DATA`.

```text
BASIC_DATA -> ADDRESS -> CONFIRMATION -> ready to submit

DRAFT -> SUBMITTED -> IN_REVIEW -> APPROVED
                              \-> REJECTED
```

Only drafts can be edited. Address details are resolved through ViaCEP from the submitted postal code. The preferred date cannot be in the past; high priority requires an estimated value of at least 100; terms must be accepted to submit.

## Authentication and authorization

- `POST /auth/register` creates a `CLIENT` account.
- `POST /auth/login` returns a JWT with the user ID and role.
- `POST /admin/users` allows an `ADMIN` to create analysts or admins and assign analyst coverage.
- Passwords are stored using BCrypt.

| Role | Access |
| --- | --- |
| `CLIENT` | Own solicitations only |
| `ANALYST` | Solicitations in covered states |
| `ADMIN` | Unrestricted |

During the bootstrap phase, health and API documentation endpoints are public; other routes require temporary HTTP Basic authentication. JWT authentication is introduced in a later phase.

## Search and consistency

Elasticsearch serves search queries only. Reads by solicitation ID and all business writes use PostgreSQL. Search results are eventually consistent, and analyst coverage is always intersected with the requested filters.

The transactional outbox records domain changes in PostgreSQL in the same transaction. A processor updates Elasticsearch asynchronously and idempotently. If Elasticsearch is unavailable, the business transaction can still succeed and pending events can be retried.

## Audit and errors

Mutation audit records identify the actor, action, aggregate, timestamp, and outcome without storing request bodies. API errors use a consistent response shape with a stable error code, message, field errors where applicable, and timestamp.

Expected HTTP mappings include `400` for malformed input, `401` for unauthenticated requests, `403` for forbidden access, `404` for missing resources, `409` for state or concurrency conflicts, `422` for business validation, and `502`/`503` for ViaCEP failures.

## Roadmap

See [`docs/implementation-plan.md`](docs/implementation-plan.md) for the full phased plan.

| Phase | Scope | Status |
| --- | --- | --- |
| 0 | Bootstrap, architecture, and environment | Complete |
| 1–17 | Domain, persistence, authentication, request workflow, search, outbox, audit, API contract, acceptance tests, and delivery | Pending |

## Assumptions and trade-offs

- `termsAccepted = false` may be saved on a draft but prevents submission.
- Analyst coverage is a set of state codes.
- Money uses `BigDecimal`; calendar dates use `LocalDate`.
- A modular monolith keeps deployment and operations simple while preserving boundaries for future extraction.
- The local Elasticsearch container runs without security for simpler development; Testcontainers uses the secured image configuration.
- Cucumber is intentionally omitted. Gherkin documents expected behavior, while JUnit executes the tests.

## Production considerations

- Replace temporary HTTP Basic authentication with JWT or an external OAuth2/OIDC provider.
- Enable Elasticsearch TLS and authentication in the local Compose environment.
- Add distributed locking or partitioning if the outbox processor runs across multiple replicas.
- Export Actuator metrics to a monitoring platform and add distributed tracing.
- Define Elasticsearch index lifecycle and retention policies.
