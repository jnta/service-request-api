# Service Request API

An API for service requests with a multi-step workflow, geographic analyst authorization, Elasticsearch search, a transactional outbox, and an audit trail.

## Challenge overview

Clients create a request and complete it in three steps: basic data, address, and confirmation. Once submitted, a request becomes read-only and enters the analyst queue. Analysts can access requests only in the states covered by their assigned regions, then start a review and approve or reject the request.

## How to run

### Requirements

- Docker and Docker Compose
- JDK 21 for local development. The project pins Java 21 in [`.mise.toml`](.mise.toml); run `mise install` when using mise.

### Start the full stack

```bash
docker compose up --build
```

Compose starts PostgreSQL, Elasticsearch, and the application. The application waits for both dependencies to become healthy. Flyway applies pending migrations at startup.

Compose enables a local-only administrator bootstrap using `admin@example.com` and `local-admin-password-123`, and a local-only JWT signing key. Override `BOOTSTRAP_ADMIN_EMAIL`, `BOOTSTRAP_ADMIN_PASSWORD`, and `JWT_SECRET_BASE64` for your own development environment. Never use the Compose defaults outside local development.

Check application health:

```bash
curl localhost:8080/actuator/health
```

The API documentation is available at the URLs listed in [API documentation](#api-documentation).

Stop the services and remove their data volumes:

```bash
docker compose down -v
```

The current user must be able to access Docker. If needed, add the user to the `docker` group and start a new login session.

### Run the application outside Docker

```bash
docker compose up -d postgres elasticsearch
export JWT_SECRET_BASE64="$(openssl rand -base64 32)"
export BOOTSTRAP_ADMIN_ENABLED=true
export BOOTSTRAP_ADMIN_EMAIL=admin@example.com
export BOOTSTRAP_ADMIN_PASSWORD='replace-with-a-local-password'
mise exec -- ./gradlew bootRun
```

## How to test

```bash
mise exec -- ./gradlew test
```

The suite includes domain and application unit tests plus acceptance tests for the complete solicitation lifecycle and its main security safeguards. The acceptance suite uses Testcontainers for PostgreSQL and Elasticsearch, WireMock for ViaCEP, and MockMvc.

Acceptance tests require Docker access. BDD scenarios are documented in [`docs/features/`](docs/features/) and implemented with JUnit; Cucumber is not used.


### HTTP acceptance with Bruno

The [Bruno collection](bruno/README.md) automates round 1 happy paths and round 2 negative/security scenarios against a running API. Open `bruno/` in Bruno, select `Local`, and run the entire collection sequentially, or use the pinned CLI:

```bash
cd bruno
npx --yes @usebruno/cli@4.2.1 run --env Local --sandbox developer
```

It creates unique clients, analysts and requests per run, uses the documented local Compose administrator, and retains generated records. Configure the environment for customized credentials and ViaCEP/state fixtures. See its [scenario matrix](bruno/SCENARIOS.md) and [execution results](bruno/RESULTS.md) for coverage, prerequisites and known failures. Reports can contain credentials and tokens and are ignored by Git. HTTP checks complement the existing Java/Testcontainers suite; they do not inspect internal audit/outbox persistence or simulate infrastructure outages.

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

Flyway owns the database schema. Hibernate uses `ddl-auto: validate` and does not modify it. The initial schema is split across `V001__create_users.sql`, `V002__create_solicitations.sql`, `V003__create_analyst_state_coverage.sql`, `V004__create_outbox_events.sql`, and `V005__create_audit_logs.sql`. The former single-file `V1__create_initial_schema.sql` was replaced by `V001`–`V003`; Flyway treats `V1` and `V001` as the same version, so databases that applied the old migration must be recreated. In local Compose, `docker compose down -v` removes both PostgreSQL and Elasticsearch data volumes. Applied or shared migrations from the restructured set onward are immutable.

## Domain model

A new solicitation starts with `status = DRAFT` and `currentStep = BASIC_DATA`.

`currentStep` is derived from the completed step data rather than stored as a second mutable state. The persistence adapter carries the JPA version separately from the domain aggregate.

The domain enforces required basic data, a complete address, valid confirmation details, accepted terms on submission, and valid workflow transitions.

## Solicitation state machine

```text
BASIC_DATA -> ADDRESS -> CONFIRMATION -> ready to submit

DRAFT -> SUBMITTED -> IN_REVIEW -> APPROVED
                              \-> REJECTED
```

Only drafts can be edited. Address details are resolved through ViaCEP from the submitted postal code. The preferred date cannot be in the past; high priority requires an estimated value of at least 100; terms must be accepted to submit.

## Multi-step workflow

Clients create a draft and complete basic data, address, and confirmation in order. Address fields are resolved from CEP; confirmation stores priority, preferred date, estimated value and terms acceptance. Once submitted, the solicitation is read-only.

- `POST /solicitations` creates a draft owned by the authenticated client. The owner ID is taken from the JWT and is never accepted from the request.
- `GET /solicitations/{id}` returns the authenticated client's own solicitation. Requests for another client's resource return `403`; unknown IDs return `404`.
- `PUT /solicitations/{id}/steps/basic-data` updates service type, title, and description for the owner of a draft.
- `PUT /solicitations/{id}/steps/address` accepts CEP, number, and complement; street, neighborhood, city, and state are resolved by ViaCEP.
- `PUT /solicitations/{id}/steps/confirmation` saves priority, preferred date, value and currency, and terms acceptance.
- `POST /solicitations/{id}/submit` submits a complete solicitation with accepted terms.

## Authentication and authorization

- `POST /auth/register` creates only a `CLIENT` account.
- `POST /auth/login` returns a JWT with the user ID and role.
- `POST /admin/users` allows an `ADMIN` to create analysts or admins and assign analyst coverage.
- `POST /analyst/solicitations/{id}/start` starts review; `POST /analyst/solicitations/{id}/decide` records an approval or rejection.
- Passwords are stored using BCrypt. JWTs are signed with an environment-provided HS256 key.

| Role | Access |
| --- | --- |
| `CLIENT` | Own solicitations only |
| `ANALYST` | Solicitations in covered states |
| `ADMIN` | Unrestricted |

Health, API documentation, registration, and login endpoints are public. `POST /admin/users` requires the `ADMIN` role; other API routes require a valid JWT.

## Elasticsearch strategy

`GET /analyst/solicitations/search` queries the Elasticsearch projection. Elasticsearch serves search queries only; reads by solicitation ID and all business writes use PostgreSQL. Search results are eventually consistent, and analyst coverage is always intersected with requested filters.

## Transactional Outbox

Solicitation changes and outbox events are committed in the same PostgreSQL transaction. A background processor updates Elasticsearch asynchronously and idempotently. If Elasticsearch is unavailable, the business transaction can still succeed and pending events are retried.

## Audit strategy

An AOP interceptor writes audit records for solicitation creation, updates, submission, review start, and analyst decisions. Records identify the actor, action, aggregate, timestamp, and outcome without storing request bodies; failed operations are recorded, while reads are not audited.

## Error contract

API errors use `{code, message, errors, timestamp}` across controller and security failures. Validation errors identify fields, and error codes are stable across malformed input, business validation, conflicts, authentication, and authorization.

Expected HTTP mappings include `400` for malformed or structurally invalid input, `401` for unauthenticated requests, `403` for forbidden access, `404` for missing resources, `409` for state or concurrency conflicts, `422` for business validation and unknown postal codes, and `503` when ViaCEP is unavailable.

## API documentation

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

The specification groups endpoints by Auth, Solicitations, Analyst, and Admin. Protected operations advertise the JWT bearer scheme; API errors use the `ApiError` schema.

### Actuator and metrics

Actuator exposes health, application info, and metrics. Health is public; metrics require authentication.

| Metric | Type | Meaning |
| --- | --- | --- |
| `solicitation.created` | Counter | Successfully created solicitations |
| `solicitation.submitted` | Counter | Successfully submitted solicitations |
| `solicitation.approved` | Counter | Successfully approved solicitations |
| `solicitation.rejected` | Counter | Successfully rejected solicitations |
| `viacep.requests` | Counter | ViaCEP lookup attempts |
| `viacep.failures` | Counter | ViaCEP integration failures |
| `outbox.pending` | Gauge | Pending PostgreSQL outbox events |
| `outbox.failures` | Counter | Failed outbox event processing attempts |

Inspect available meter names at `/actuator/metrics`; query an individual meter at `/actuator/metrics/{name}` with an authenticated bearer token.

## Assumptions

- `termsAccepted = false` may be saved on a draft but prevents submission.
- Analyst coverage is a set of state codes.
- Money is represented as integer minor units plus an explicit currency (`BRL`); `BigDecimal` is reserved for boundary conversion. Calendar dates use `LocalDate`.

## Trade-offs

- A modular monolith keeps deployment and operations simple while preserving boundaries for future extraction.
- The local Elasticsearch container runs without security for simpler development; Testcontainers uses the secured image configuration.
- Cucumber is intentionally omitted. Gherkin documents expected behavior, while JUnit executes the retained unit tests.

## Production evolution

- Replace the current HS256 signing key with managed key rotation or an external OAuth2/OIDC provider.
- Enable Elasticsearch TLS and authentication in the local Compose environment.
- Add distributed locking or partitioning if the outbox processor runs across multiple replicas.
- Export Actuator metrics to a monitoring platform and add distributed tracing.
- Define Elasticsearch index lifecycle and retention policies.
