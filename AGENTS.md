# AGENTS.md

Instructions for contributors and coding agents. Architecture decisions are in [ADRs](docs/adr/), expected behavior is in [BDD features](docs/features/), and the system overview is in the [README](README.md).

## Stack

Java 21 · Spring Boot 4.1.1 (Framework 7, Hibernate 7, Jackson 3, JUnit 6) · Gradle 9.8.1 · PostgreSQL 17 · Elasticsearch 9.3.3 · Testcontainers 2.0.5

Spring Boot 4 changed several package names from Boot 3 (for example, `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`). Do not assume Boot 3 imports.

## Commands

```bash
./gradlew compileJava compileTestJava   # quick compile check
./gradlew test                          # requires Docker
docker compose up --build               # full stack
```

`DockerClientProviderStrategy` failures indicate a Docker setup issue, not an application failure.

## Architectural constraints

1. No dual writes: persist outbox events in the same transaction as domain changes. Never index into Elasticsearch inside that transaction.
2. PostgreSQL is the source of truth; Elasticsearch is a rebuildable projection.
3. Domain code must not depend on Spring, JPA, Jackson, or HTTP.
4. Inject `Clock` wherever domain behavior depends on time. Never read the system clock directly.
5. Aggregates expose business operations (`submit`, `approve`), not public state setters.
6. Derive `clientId` from the authenticated identity, never from the request body.
7. Intersect search filters with analyst coverage; query parameters must never expand authorization.
8. `shared` must not depend on other modules.
9. Flyway owns the schema. Never edit an applied migration or change `ddl-auto`.

## Module and domain structure

- Organize top-level packages by business capability. Within a module, separate `domain`, `application`, and `adapter` responsibilities as they emerge; do not create empty layers in advance.
- Keep a pure domain model. Application use cases orchestrate work and depend on ports; adapters handle HTTP, persistence, and external services.
- Group domain code by cohesive concept rather than putting every type in one flat package. In the solicitation module, keep concepts such as address, basic data, confirmation, review, and workflow distinct.
- The aggregate root coordinates its invariants and state transitions. Value objects own and normalize the invariants of the value they represent.
- Put cross-field business rules with the domain concept that owns those fields. Keep request-shape validation at the HTTP boundary, but never rely on it as the only enforcement of a domain invariant.
- Validate an operation before mutating the aggregate so a rejected update cannot leave partial state behind.
- Prefer deriving values such as workflow progress or terminal status from authoritative state over storing duplicate mutable state.
- Use shared types only when concepts are genuinely shared across modules. `shared` is a small kernel, not a general-purpose model or utility bucket.
- Add ports at external or module boundaries where substitution matters; do not introduce an interface for every class.

## Code and design

- Avoid comments and do not write JavaDoc. Prefer clear names, small methods, and tests that express intent.
- Keep code, BDD scenarios, README, and ADRs in English.
- Use `BigDecimal` for money, `LocalDate` for calendar dates, and `Instant` for timestamps.
- Represent business failures as domain exceptions and map them to HTTP in `GlobalExceptionHandler`.
- Name migrations `V<n>__description.sql`.
- Apply Clean Code and Object Calisthenics principles when they improve clarity; do not follow rules mechanically.
- Prefer single-responsibility objects, short methods, shallow indentation, and early returns when they simplify control flow.
- Use domain types where they prevent invalid combinations or ambiguous primitives. Keep behavior with the object that owns the data; avoid long call chains.
- Do not add artificial abstractions just to satisfy a rule. Readability and simplicity take precedence.

## Tests

- Follow the test pyramid: many domain unit tests, some integration tests, and few end-to-end tests.
- Name tests after behavior, e.g. `shouldRejectWhenAddressIsMissing`.
- Make `@DisplayName` mirror the related scenario in `docs/features/*.feature`.
- Use Testcontainers for integration tests; never hard-code hosts, ports, or passwords.

## Documentation

| File | Purpose |
| --- | --- |
| `README.md` | What the system does, how to run and test it |
| `docs/adr/` | Why architectural decisions were made |
| `docs/features/` | Expected behavior in Gherkin; not executed by Cucumber |

Do not create `package-info.java` or ADRs for implementation details.

## Workflow

- Work on one phase at a time, following [`docs/implementation-plan.md`](docs/implementation-plan.md).
- Do not reformat or reorder code outside the task scope.
- Do not commit or push unless explicitly requested.
- Done means implementation, tests, a BDD scenario, no regressions, and README/ADR updates when architecture changes.
