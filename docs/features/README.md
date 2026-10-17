# Feature files (BDD)

These files describe the expected behaviour of the system in Gherkin.

**They are not executed by Cucumber.** BDD here is a way of thinking and naming behaviour, not
a dependency: the build stays simple and the feedback loop fast, without giving up business
language.

Unit scenarios are implemented as JUnit tests whose `@DisplayName` mirrors the scenario, usually
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

Integration and end-to-end scenarios are deferred to phases 15–16 and will be reintroduced there.

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
| `analyst-review.feature` | 10 | Review start, decisions, coverage, and conflicts |
| `solicitation-search.feature` | 11 | Elasticsearch filters, pagination, and coverage enforcement |
| `transactional-outbox.feature` | 12 | Atomic event persistence, retries, and idempotent indexing |
| `audit-trail.feature` | 13 | AOP audit records, success outcomes, and read exclusion |
| `api-error-contract.feature` | 14 | Stable error format, status mappings, and field validation details |
| `developer-experience.feature` | 17 | Full-stack startup, health, migrations, and API documentation |
