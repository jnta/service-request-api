# Bruno HTTP acceptance collection

Open this directory as a collection in Bruno and select **Local**, then run the entire collection sequentially. Do not run folders in parallel: round 2 consumes fixtures from setup and round 1. A single request can be replayed after its prerequisites have run in the same session.

## Prerequisites and execution

Start the application, PostgreSQL and Elasticsearch using the repository's Compose instructions. The outbox processor must be enabled. ViaCEP must be reachable by the application. Node.js is required for the CLI.

The Local environment mirrors the documented Compose-only administrator credentials. Override `adminEmail` and `adminPassword` for a customized bootstrap account; these variables do not create the initial administrator. Never point this collection at production. For private values, copy Local to `environments/Private.bru` (ignored) or use CLI overrides rather than committing credentials.

```bash
cd bruno
npx --yes @usebruno/cli@4.2.1 run --env Local --sandbox developer
```

Optional local reports (contain credentials/tokens and response data; do not share or commit):

```bash
mkdir -p reports
npx --yes @usebruno/cli@4.2.1 run --env Local --sandbox developer --reporter-json reports/results.json
```

The developer sandbox permits the reviewed collection scripts and their bounded asynchronous polling. No external script libraries are required. In the desktop runner, allow the developer scripting mode if prompted.

## Data and assertions

Each full run generates unique email addresses and dates relative to the current time. Tokens and IDs are runtime variables, never saved into environment files. It creates two clients, two analysts, and three solicitations, retaining all fixtures because the API has no delete endpoint. Repeated runs do not delete or reset existing application data. Requests intentionally add successful and failed audit events.

`cep` and `expectedState` must identify a real ViaCEP address; defaults use 01001000/SP. `outsideState` must differ. Search checks use fixture IDs, `q`, `dateFrom`, and `dateTo`, assert the `total` response field, and never assume an empty database. They poll at most 20 retries with one second between retries. A polling timeout is a failure rather than a skipped assertion. The fixture must appear in a covered analyst's search before the uncovered analyst search assertion; this prevents a misleading pass caused by a missing projection. Large shared databases with over 100 matching records can place a fixture outside the queried page; use an isolated acceptance database.

Every request asserts HTTP status. Negative requests also enforce `{code, message, errors, timestamp}`; application error codes are asserted where defined in source. Readbacks verify that invalid steps, incomplete submissions, declined terms and invalid decisions preserve state. Request names and their expectations are maintained alongside the collection files.

- Setup: public health, client registration and login, admin login, managed analysts with distinct state coverage.
- Round 1: draft ownership, three ordered steps, ViaCEP resolution, submission, eventual search, covered review, approval and rejection, terminal reads.
- Round 2: missing/invalid authentication, wrong password, duplicate/invalid registration, malformed JSON, role authorization and read-route separation, cross-client reads/writes/submission, geographic reads/review/decision/search, missing resources, immutable terminal requests, invalid transitions/search input, step ordering, incomplete drafts, unsupported service type, normalized text-length boundaries, invalid CEP, past dates, high-priority threshold, money precision/value/currency, required fields, rejected terms, and decision-comment length.

## Contract sources and boundaries

Built against the running `/v3/api-docs`, request DTOs, `SecurityConfiguration`, domain invariants, `GlobalExceptionHandler`, BDD features, and the existing Java acceptance suite. Password size is **8–72 characters** in the current request DTO, regardless of earlier design discussions.

The previous conversation did not expose the text defining rounds 1 and 2, only reference markers. Here round 1 means happy-path HTTP workflows and round 2 means negative/security HTTP acceptance, as requested.

This collection does not claim to verify database/outbox/audit atomicity, BCrypt storage, optimistic-lock races, expired JWTs, or a deterministic ViaCEP outage. Those require controlled infrastructure, signing/clock fixtures or the existing Java/Testcontainers tests. Unknown CEP depends on ViaCEP returning not-found; a 503 is an environment/integration blocker, never accepted as a passing 422. Search verifies the observable projection, not internal delivery mechanics. No application code or configuration is changed by this collection.

CLI and scripting references: [Bruno CLI](https://github.com/usebruno/bruno/blob/main/packages/bruno-cli/readme.md), [JavaScript API](https://github.com/usebruno/bruno-docs/blob/main/testing/script/javascript-reference.mdx).

Token and created-resource ID capture runs in Post Response, independently of test execution. Values remain runtime variables: inspect them in Bruno’s runtime variable view, not in the Local environment file. Run setup and dependent requests in the same collection run.
