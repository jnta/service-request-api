# Service Request API

An API for service requests with a multi-step workflow, geographic analyst authorization, Elasticsearch search, a transactional outbox, and an audit trail.

## Quick start (Docker Compose)

This is the recommended way to run the application. Docker builds the Java application, so a local JDK, Gradle, and `mise` are **not required** for this setup.

### Requirements

- Docker Engine or Docker Desktop with the Docker Compose v2 plugin, running and accessible to your user
- Free host ports `8080`, `5432`, and `9200`
- Enough memory assigned to Docker to run Elasticsearch and the API
- Internet access on the first run to download container images and Gradle dependencies
- `curl` for the health check below (or open the health URL in a browser)

Start PostgreSQL, Elasticsearch, and the application:

```bash
docker compose up --build -d
```

The first build can take a few minutes. Wait until all three services show `healthy`:

```bash
docker compose ps
```

Verify the application:

```bash
curl --fail http://localhost:8080/actuator/health
```

The expected response is `{"status":"UP"}`. Compose waits for PostgreSQL and Elasticsearch health checks before starting the application, and Flyway applies pending database migrations automatically.

On Windows PowerShell, open the health URL in a browser or run the command as `curl.exe --fail http://localhost:8080/actuator/health`.

### Local URLs and credentials

These credentials and the JWT signing key are for local development only. Never use the Compose defaults outside a local environment.

| Service | Address | Local credentials |
| --- | --- | --- |
| API | <http://localhost:8080> | — |
| Swagger UI | <http://localhost:8080/swagger-ui.html> | — |
| OpenAPI JSON | <http://localhost:8080/v3/api-docs> | — |
| PostgreSQL / DBeaver | `localhost:5432` | Database `solicitations`, user `solicitations`, password `solicitations` |
| Elasticsearch | <http://localhost:9200> | No authentication in local Compose |
| Bootstrap administrator | `POST /auth/login` | `admin@example.com` / `local-admin-password-123` |

When connecting from DBeaver on the host, use `localhost`, not the Docker service name `postgres`. The service name is only resolvable by containers on the Compose network.

Quickly verify administrator login:

```bash
curl --fail --silent --show-error \
  --request POST http://localhost:8080/auth/login \
  --header 'Content-Type: application/json' \
  --data '{"email":"admin@example.com","password":"local-admin-password-123"}'
```

The response contains an access token. Compose defaults can be overridden with `BOOTSTRAP_ADMIN_NAME`, `BOOTSTRAP_ADMIN_EMAIL`, `BOOTSTRAP_ADMIN_PASSWORD`, and `JWT_SECRET_BASE64` environment variables. Bootstrap creates the configured administrator if it does not already exist; changing these variables does not change the password of an existing account. Keep custom values local and do not commit them.

The login command above uses Bash syntax. On Windows, run it from WSL/Git Bash or use the Bruno collection below.

### Stop or reset the local stack

Stop containers while **keeping** the database and search data:

```bash
docker compose down
```

To intentionally delete all local PostgreSQL and Elasticsearch data and start fresh:

```bash
docker compose down -v
```

> **Warning:** `down -v` permanently deletes the local database and search volumes, including users and solicitations. Do not use it if you need to preserve local data.

## Local development (run the application outside Docker)

This path requires JDK 21. The Gradle Wrapper downloads and runs the pinned Gradle version; installing Gradle separately is unnecessary. Java 21 is pinned in [`.mise.toml`](.mise.toml); `mise install` is optional if you use mise to manage Java. The example below uses Bash and OpenSSL to generate the local JWT key.

Start only the infrastructure containers:

```bash
docker compose up -d postgres elasticsearch
docker compose ps
```

Wait until both infrastructure services show `healthy`. Then, from the repository root, configure a local JWT key and bootstrap administrator and run the application:

```bash
export JWT_SECRET_BASE64="$(openssl rand -base64 32)"
export BOOTSTRAP_ADMIN_ENABLED=true
export BOOTSTRAP_ADMIN_EMAIL=admin@example.com
export BOOTSTRAP_ADMIN_PASSWORD='replace-with-a-local-password'
./gradlew bootRun
```

If Java 21 is managed with mise, run `mise install` once and replace the last command with `mise exec -- ./gradlew bootRun`. This setup uses the default local PostgreSQL and Elasticsearch addresses from `application.yml`.

On Windows PowerShell, use this equivalent after starting the infrastructure services:

```powershell
$env:JWT_SECRET_BASE64 = (openssl rand -base64 32)
$env:BOOTSTRAP_ADMIN_ENABLED = "true"
$env:BOOTSTRAP_ADMIN_EMAIL = "admin@example.com"
$env:BOOTSTRAP_ADMIN_PASSWORD = "replace-with-a-local-password"
.\gradlew.bat bootRun
```

This local-development example uses OpenSSL to generate a JWT key. The recommended Compose quick start does not require OpenSSL.

## Tests

Requirements: JDK 21 and a running Docker Engine that the current user can access. The test suite uses Testcontainers to start its own PostgreSQL and Elasticsearch containers, so you do **not** need to start the Compose application first. The first test run may download Gradle dependencies and container images.

```bash
./gradlew test
```

On Windows PowerShell, use `.\gradlew.bat test` instead. The suite includes domain and application unit tests plus acceptance tests for the solicitation lifecycle and security safeguards. Acceptance tests use Testcontainers, WireMock for ViaCEP, and MockMvc. Docker client/provider errors indicate a Docker setup issue, not an application test failure. If you use mise to manage Java, run `mise install` and then `mise exec -- ./gradlew test`.

BDD scenarios are documented in [`docs/features/`](docs/features/) and implemented with JUnit; Cucumber is not used.

## Optional HTTP acceptance tests (Bruno)

The [Bruno collection guide](bruno/README.md) covers prerequisites, execution, and data retention. It runs against a started API and checks the happy paths and negative/security scenarios. It requires Node.js, outbound access to ViaCEP for address requests, and a reachable Compose administrator. Run the collection sequentially:

```bash
cd bruno
npx --yes @usebruno/cli@4.2.1 run --env Local --sandbox developer
```

The collection creates unique clients, analysts, and solicitations and retains them between runs. Reports can contain credentials and tokens; they are ignored by Git and should not be shared. HTTP checks complement the Java/Testcontainers suite; they do not inspect internal audit/outbox persistence or simulate infrastructure outages. See the [collection guide](bruno/README.md) for execution and coverage boundaries.

## What the application does

Clients create a request and complete it in three steps: basic data, address, and confirmation. Once submitted, a request becomes read-only and enters the analyst queue. Analysts can access requests only in the states covered by their assigned regions, then start a review and approve or reject the request.

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
- `POST /solicitations/{id}/submit` lets the owning `CLIENT` or an `ADMIN` submit a complete DRAFT with accepted terms. An administrative submission preserves the original client as owner and audits the ADMIN as the actor.

ViaCEP connection and response timeouts default to `2s` and `3s`; configure them with `VIACEP_CONNECT_TIMEOUT` and `VIACEP_READ_TIMEOUT`. A timeout returns `503` and does not complete the address step.

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
