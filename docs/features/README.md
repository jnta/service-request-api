# Feature files (BDD)

These files describe the expected behaviour of the system in Gherkin.

**They are not executed by Cucumber.** BDD here is a way of thinking and naming behaviour, not
a dependency: the build stays simple and the feedback loop fast, without giving up business
language.

Each scenario is implemented as a JUnit test whose `@DisplayName` mirrors the scenario, usually
shaped like:

```java
@Nested
class WhenClientSubmitsSolicitation {

    @Test
    void shouldSubmitWhenAllStepsAreComplete() { }

    @Test
    void shouldRejectWhenAddressIsMissing() { }

    @Test
    void shouldRejectWhenTermsAreNotAccepted() { }
}
```

Acceptance scenarios (end-to-end) live in `src/test/java/com/jnta/acceptance` and use
Testcontainers (PostgreSQL + Elasticsearch), WireMock (ViaCEP) and REST Assured/MockMvc.

## Index

| File | Phase | Contents |
| ---- | ----- | -------- |
| `application-bootstrap.feature` | 0 | Bootstrap and health check |
| `solicitation-domain.feature` | 1 | Solicitation invariants and workflow |
| `solicitation-persistence.feature` | 2 | PostgreSQL round-trip and optimistic locking |
| `authentication.feature` | 3 | User registration, JWT authentication, roles, and analyst coverage |
| `solicitation-api.feature` | 4–5 | Authenticated creation, ownership, retrieval, and basic-data update |
| `address-step.feature` | 6 | ViaCEP lookup and address-step behavior |
| `confirmation-step.feature` | 7 | Confirmation data and validation |
| `submission.feature` | 8 | Completeness, terms, ownership, and submission transitions |
| `analyst-authorization.feature` | 9 | Client ownership, analyst state coverage, and admin access |
