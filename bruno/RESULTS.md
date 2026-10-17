# Execution results — 2026-10-17

Executed against the running local Compose application at `http://localhost:8080`, using Bruno CLI 4.2.1 and the live OpenAPI 3.1.0 contract (12 paths). Application, PostgreSQL and Elasticsearch were healthy. ViaCEP resolved the address and unknown postal code as expected. No application code or runtime configuration was changed.

## Final run

- 84 request files; 88 HTTP executions including bounded search retries.
- 87 successful request executions, 1 failing request execution.
- 220 of 221 scripted checks passed; process exit code 1.
- Setup, approval/rejection lifecycles, eventual search and remaining negative/security scenarios passed.
- Failure: **Invalid bearer token** returns the expected HTTP 401 but lacks the documented `{code, message, errors, timestamp}` error envelope. The collection intentionally retains the failing assertion.

Spring Security's resource-server JWT failure handler appears to bypass the custom entry point configured through `exceptionHandling` in `SecurityConfiguration`. This is a source-based diagnosis, not a fix verified by this task.

## Earlier complete run

The first complete execution had 86 HTTP executions and 215/218 checks passing. Alongside the invalid-token envelope failure, the first covered search returned HTTP 500. Application logs at `2026-10-17T22:39:49.862Z` reported:

```text
[es/indices.create] failed: [resource_already_exists_exception] index [solicitations/...] already exists
```

The search/index adapter checks index existence before creation, while outbox processing can also initialize it. The evidence is consistent with concurrent index creation. Search succeeded on the final run once the index existed. No index was deleted, and this report does not claim a deterministic reproduction of the race. HTTP 500 is deliberately not treated as an eventual-consistency success or retried away.

An initial collection-format smoke run was discarded after correcting Bruno block indentation; the results above refer to the two complete runs of all 84 request files.

## Limits and retained data

The former conversation exposed only reference markers for its assistant plans; scope was reconstructed from the explicit request, live contract, source and feature scenarios. Fixtures from the complete runs remain in the local database. They use generated `bruno-*` emails. No database reset, delete, commit or push was performed. Raw execution reports remain task-local because they contain credentials and tokens; the delivered JSON summary omits headers and bodies.

There is no environment blocker preventing execution. The suite remains red because the invalid-token response violates the documented error contract. Database/audit/outbox internals, deterministic dependency outages, expired-token fixtures and concurrent locking are outside these HTTP scenarios and remain covered separately as described in the collection README.
