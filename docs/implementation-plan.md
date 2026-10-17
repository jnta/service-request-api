Fase 0 — Bootstrap, arquitetura base e ambiente — COMPLETED

## Objetivo

Criar uma fundação onde qualquer desenvolvedor consiga clonar, executar e testar o projeto sem configuração manual.

## Implementação

Criar o projeto com:

```
Java 21
Spring Boot
Spring Web
Spring Security
Spring Data JPA
PostgreSQL
Spring Data Elasticsearch
Flyway
Bean Validation
Springdoc OpenAPI
Actuator
Testcontainers
JUnit 5
Mockito
```

Estrutura inicial:

```
src/main/java/com/jnta
├── auth
├── user
├── solicitation
├── audit
├── search
├── shared
└── ChallengeApplication.java
```

Infraestrutura:

```
Dockerfile
compose.yaml
README.md
```

Containers:

```
app
postgres
elasticsearch
```

Configuração:

```
application.yml
application-test.yml
```

Adicionar:

```
@Bean
Clock clock() {
    return Clock.systemUTC();
}
```

Healthcheck:

```
GET /actuator/health
```

Flyway começa pelo menos com uma migration de infraestrutura ou schema inicial.

## Decisões que já devem estar documentadas

No README:

```
Architecture Style: Modular Monolith

Transactional database:
PostgreSQL

Search engine:
Elasticsearch

PostgreSQL is the source of truth.
Elasticsearch is a derived search projection.
```

## Testes técnicos

Smoke test:

```
@SpringBootTest
class ApplicationContextTest {

    @Test
    void shouldLoadApplicationContext() {
    }
}
```

Testcontainers para validar conexão real:

```
@Testcontainers
class InfrastructureIntegrationTest {
}
```

## BDD da fase

```
Feature: Application bootstrap

  Scenario: Starting the application with all dependencies
    Given PostgreSQL is available
    And Elasticsearch is available
    When the application starts
    Then the Spring context should load successfully
    And the health endpoint should report UP
```

Outro:

```
Scenario: Applying database migrations
  Given an empty PostgreSQL database
  When the application starts
  Then Flyway should apply all pending migrations
  And the application should start without schema errors
```

## Critério de aceite

A fase acaba quando:

```
docker compose up --build
```

sobe tudo e:

```
curl localhost:8080/actuator/health
```

retorna healthy.

---

# Fase 1 — Domínio puro da Solicitação — COMPLETED

Essa é uma das fases mais importantes.

Ainda não precisamos de controller.

## Objetivo

Modelar corretamente o comportamento da solicitação antes de envolver Spring, HTTP ou banco.

## Objetos de domínio

Criaria:

```
Solicitation
SolicitationId
SolicitationStatus
SolicitationStep
ServiceType
Priority
Address
BrazilianState
Cep
Decision
```

Estado inicial:

```
status = DRAFT
currentStep = BASIC_DATA
```

## API comportamental do aggregate

Algo próximo de:

```
public class Solicitation {

    public static Solicitation create(
        SolicitationId id,
        UserId owner
    );

    public void updateBasicData(
        ServiceType serviceType,
        String title,
        String description
    );

    public void updateAddress(Address address);

    public void updateConfirmation(
        Priority priority,
        LocalDate preferredDate,
        Money estimatedValue,
        boolean termsAccepted
    );

    public void submit(Clock clock);

    public void startReview();

    public void approve(String comment);

    public void reject(String comment);
}
```

Nada de:

```
setStatus()
setCurrentStep()
```

exposto publicamente.

## Invariantes

### Basic Data

```
serviceType obrigatório
title 3–80
description 20–1000
```

### Address

```
CEP válido
number 1–20
complement opcional
```

### Confirmation

```
priority obrigatória
preferredDate >= hoje
termsAccepted == true para submit
HIGH => estimatedValue >= 100
```

### Workflow

```
DRAFT
  ↓
SUBMITTED
  ↓
IN_REVIEW
  ↓
APPROVED / REJECTED
```

### Editing

Somente:

```
status == DRAFT
```

pode sofrer alteração de dados.

## Testes unitários

Aqui eu faria muitos.

### Creation

```
@Test
void shouldCreateSolicitationAsDraft() {
}
```

```
Scenario: Creating a solicitation
  Given a valid client
  When a solicitation is created
  Then its status should be DRAFT
  And its current step should be BASIC_DATA
```

### Step 1

```
Scenario: Completing basic data
  Given a solicitation in DRAFT
  And its current step is BASIC_DATA
  When valid basic data is provided
  Then the current step should become ADDRESS
```

Validação:

```
Scenario: Rejecting invalid basic data
  Given a solicitation in DRAFT
  When a title shorter than 3 characters is provided
  Then the basic data should be rejected
  And the current step should remain BASIC_DATA
```

### Step 2

```
Scenario: Completing address
  Given a solicitation whose basic data is complete
  When a valid address is provided
  Then the current step should become CONFIRMATION
```

### Step 3

```
Scenario: Completing confirmation
  Given a solicitation whose address is complete
  When valid confirmation data is provided
  Then the solicitation should become ready for submission
```

### High priority

```
Scenario: Rejecting low estimated value for high priority
  Given a solicitation in DRAFT
  When priority HIGH is selected
  And estimatedValue is lower than 100
  Then the confirmation should be rejected
```

### Submit

```
Scenario: Submitting a complete solicitation
  Given all solicitation steps are complete
  And terms have been accepted
  When the client submits the solicitation
  Then its status should become SUBMITTED
```

```
Scenario: Rejecting incomplete submission
  Given a solicitation missing address information
  When the client submits the solicitation
  Then submission should fail
  And status should remain DRAFT
```

### Edit after submit

```
Scenario: Preventing modifications after submission
  Given a solicitation with status SUBMITTED
  When the client attempts to update the basic data
  Then the update should be rejected
```

### Review

```
Scenario: Starting review
  Given a solicitation with status SUBMITTED
  When an analyst starts the review
  Then its status should become IN_REVIEW
```

### Approve

```
Scenario: Approving a solicitation
  Given a solicitation with status IN_REVIEW
  When an analyst approves it
  Then its status should become APPROVED
```

### Invalid transition

```
Scenario: Preventing approval of a draft
  Given a solicitation with status DRAFT
  When an analyst attempts to approve it
  Then the operation should be rejected
```

## Critério de aceite

Todo o workflow deve ser testável **sem Spring**.

Essa é uma ótima regra arquitetural.

---

# Fase 2 — Persistência PostgreSQL — COMPLETED

## Objetivo

Persistir o domínio sem deixar JPA ditar suas regras.

## Migration

Eu criaria:

```
users
analyst_states
solicitations
```

Talvez endereço embutido na tabela:

```
solicitations
-------------
id
client_id
service_type
title
description

cep
street
neighborhood
city
state
number
complement

priority
preferred_date
estimated_value_minor_units
estimated_value_currency
terms_accepted

status
current_step

version

created_at
updated_at
```

## Repository

Port:

```
public interface SolicitationRepository {

    Solicitation save(Solicitation solicitation);

    Optional<Solicitation> findById(SolicitationId id);
}
```

Adapter:

```
JpaSolicitationRepository
SolicitationJpaEntity
SolicitationJpaMapper
```

Não exporia Spring Data diretamente para application/domain.

## Optimistic locking

No JPA:

```
@Version
private Long version;
```

## Testes

Repository integration test com PostgreSQL real via Testcontainers.

### BDD

```
Feature: Solicitation persistence

  Scenario: Persisting a draft solicitation
    Given a newly created solicitation
    When the solicitation is saved
    And the solicitation is loaded again
    Then its status should still be DRAFT
    And its current step should still be BASIC_DATA
```

```
Scenario: Persisting partially completed workflow
  Given a solicitation with completed basic data
  When the solicitation is saved
  And later loaded
  Then the basic data should be preserved
  And the current step should be ADDRESS
```

### Optimistic locking

```
Scenario: Detecting concurrent modifications
  Given two transactions load the same solicitation
  When the first transaction updates and commits it
  And the second transaction attempts to update the stale version
  Then the second update should fail with an optimistic locking conflict
```

## Critério de aceite

O domínio sobrevive a um ciclo:

```
domain
↓
database
↓
domain
```

sem perder significado.

---

# Fase 3 — Usuários, roles e autenticação JWT — COMPLETED

## Objetivo

Estabelecer identidade e autorização básica.

## Domínio

```
User
UserId
Role
```

Roles:

```
CLIENT,
ANALYST,
ADMIN
```

Analyst:

```
Set<State> coverage
```

## Endpoints

```
POST /auth/register
POST /auth/login
POST /admin/users
```

### Register

Só cria:

```
CLIENT
```

Exemplo:

```
{
  "name": "Jonata",
  "email": "jonata@example.com",
  "password": "..."
}
```

### Login

Retorna:

```
{
  "accessToken": "...",
  "tokenType": "Bearer"
}
```

## Segurança

Password:

```
BCrypt
```

JWT:

```
{
  "sub": "<uuid>",
  "role": "CLIENT"
}
```

Coverage de analyst fica no banco.

## Testes

### Register

```
Scenario: Registering a client
  Given an email that is not registered
  When a user registers
  Then a CLIENT account should be created
  And the password should not be stored in plain text
```

### Duplicate

```
Scenario: Rejecting duplicate email registration
  Given an existing user with email "user@email.com"
  When another registration uses the same email
  Then the registration should fail with conflict
```

### Login

```
Scenario: Authenticating valid credentials
  Given a registered client
  When the client submits the correct credentials
  Then a valid JWT should be returned
```

### Invalid login

```
Scenario: Rejecting invalid credentials
  Given a registered user
  When an incorrect password is provided
  Then authentication should fail
```

### Admin

```
Scenario: Admin creates an analyst
  Given an authenticated ADMIN
  When the admin creates a user with role ANALYST
  And assigns DF and GO as coverage
  Then the analyst should be created
  And the analyst should have coverage for DF and GO
```

### Privilege escalation

```
Scenario: Client cannot create an admin
  Given an authenticated CLIENT
  When the client calls the admin user creation endpoint
  Then access should be denied
```

---

# Fase 4 — Criar e consultar Solicitação — COMPLETED

Agora temos o primeiro fluxo HTTP real.

## Endpoints

```
POST /solicitations
GET /solicitations/{id}
```

## Application

Criar:

```
CreateSolicitationUseCase
GetSolicitationUseCase
```

Input:

```
CreateSolicitationCommand
```

Output:

```
SolicitationResponse
```

## Ownership

Ao criar:

```
clientId = authenticated user
```

Nunca aceitar:

```
{
  "clientId": "..."
}
```

do request.

## Testes BDD

```
Feature: Solicitation ownership

  Scenario: Client creates a solicitation
    Given an authenticated CLIENT
    When the client creates a solicitation
    Then a new solicitation should be created
    And the authenticated client should be its owner
    And its status should be DRAFT
```

```
Scenario: Client reads own solicitation
  Given a CLIENT owns a solicitation
  When the client requests that solicitation
  Then the solicitation should be returned
```

```
Scenario: Client cannot read another client's solicitation
  Given client A owns a solicitation
  And client B is authenticated
  When client B requests client A's solicitation
  Then access should be denied
```

```
Scenario: Unknown solicitation
  Given an authenticated client
  When a non-existing solicitation is requested
  Then the API should return 404
```

---

# Fase 5 — Step 1: Dados básicos — COMPLETED

Aqui eu prefiro uma fase por etapa inicialmente. Facilita bastante verificar comportamento.

## Endpoint

```
PUT /solicitations/{id}/steps/basic-data
```

Request:

```
{
  "serviceType": "INSTALLATION",
  "title": "Instalação de equipamento",
  "description": "..."
}
```

## Fluxo

```
Controller
↓
UpdateBasicDataUseCase
↓
ownership policy
↓
Solicitation.updateBasicData()
↓
repository.save()
```

## BDD

```
Feature: Basic data step

  Scenario: Owner completes basic data
    Given the client owns a DRAFT solicitation
    When valid basic data is submitted
    Then the basic data should be persisted
    And currentStep should become ADDRESS
```

```
Scenario: Rejecting short title
  Given the client owns a DRAFT solicitation
  When a title shorter than 3 characters is submitted
  Then the API should return a validation error
  And currentStep should remain BASIC_DATA
```

```
Scenario: Rejecting modification by another client
  Given client A owns a solicitation
  When client B tries to update its basic data
  Then access should be denied
```

```
Scenario: Rejecting modification after submission
  Given a solicitation is SUBMITTED
  When its owner attempts to change basic data
  Then the API should return a business conflict
```

---

# Fase 6 — Step 2 + ViaCEP — COMPLETED

## Endpoint

```
PUT /solicitations/{id}/steps/address
```

Request ideal:

```
{
  "cep": "70040902",
  "number": "123",
  "complement": "Sala 4"
}
```

O cliente não envia:

```
street
city
state
neighborhood
```

Esses vêm do ViaCEP.

## Port

```
public interface PostalCodeGateway {

    PostalCodeResult find(Cep cep);
}
```

Adapter:

```
ViaCepClient
```

## Regras

Primeiro consulta ViaCEP.

Somente depois modifica o aggregate.

## BDD

```
Feature: Address step

  Scenario: Completing address using ViaCEP
    Given a solicitation whose current step is ADDRESS
    And ViaCEP returns a valid address for the provided CEP
    When the client submits the CEP and number
    Then the address should be populated using ViaCEP data
    And currentStep should become CONFIRMATION
```

```
Scenario: ViaCEP returns an invalid CEP
  Given a solicitation whose current step is ADDRESS
  And ViaCEP reports the CEP does not exist
  When the client submits that CEP
  Then the address step should fail
  And no partial address should be persisted
  And currentStep should remain ADDRESS
```

```
Scenario: ViaCEP is unavailable
  Given ViaCEP is unavailable
  When the client attempts to complete the address
  Then the API should return an integration error
  And the solicitation should remain unchanged
```

```
Scenario: Rejecting address step before basic data
  Given a solicitation whose current step is BASIC_DATA
  When the client attempts to submit address data
  Then the operation should be rejected
```

WireMock seria perfeito aqui.

---

# Fase 7 — Step 3: Confirmação — COMPLETED

## Endpoint

```
PUT /solicitations/{id}/steps/confirmation
```

Request:

```
{
  "priority": "HIGH",
  "preferredDate": "2026-11-20",
  "estimatedValue": {
    "amount": "350.00",
    "currency": "BRL"
  },
  "termsAccepted": true
}
```

## BDD

```
Feature: Confirmation step

  Scenario: Completing confirmation
    Given a solicitation whose current step is CONFIRMATION
    When valid confirmation data is submitted
    Then the confirmation should be persisted
    And the solicitation should become ready for submission
```

```
Scenario: Preferred date is in the past
  Given today is 2026-10-16
  When preferredDate is 2026-10-15
  Then the confirmation should be rejected
```

Esse teste usa `Clock.fixed()`.

```
Scenario: High priority requires minimum estimated value
  Given priority is HIGH
  When estimatedValue is below 100
  Then the confirmation should be rejected
```

```
Scenario: Terms are not accepted
  Given all other confirmation fields are valid
  When termsAccepted is false
  Then the data may be stored as draft
  But submission must not be allowed
```

Aqui precisamos tomar uma decisão de domínio: eu permitiria armazenar `false` no draft e impediria submit.

---

# Fase 8 — Submit completo — COMPLETED

## Endpoint

```
POST /solicitations/{id}/submit
```

Sem payload.

## Application

```
SubmitSolicitationUseCase
```

## Validação

O aggregate inteiro precisa estar consistente.

Idealmente retorna todas as violações relevantes.

## BDD

```
Feature: Submission

  Scenario: Submitting a complete solicitation
    Given a client owns a complete DRAFT solicitation
    When the client submits it
    Then its status should become SUBMITTED
    And it should no longer be editable
```

```
Scenario: Rejecting incomplete solicitation
  Given a solicitation does not have a completed address
  When the owner submits it
  Then the API should return 422
  And status should remain DRAFT
```

```
Scenario: Submitting twice
  Given a solicitation is already SUBMITTED
  When the owner submits it again
  Then the API should return 409
```

```
Scenario: Non-owner submits solicitation
  Given client A owns a complete solicitation
  When client B attempts to submit it
  Then access should be denied
```

---

# Fase 9 — Autorização de Analyst por UF — COMPLETED

Antes de implementar decisão, eu faria a policy.

## Componente

```
SolicitationAccessPolicy
```

Métodos:

```
canRead(...)
canEdit(...)
canReview(...)
canDecide(...)
```

## Regra

```
CLIENT
→ own resource

ANALYST
→ solicitation.state ∈ coverage

ADMIN
→ unrestricted
```

## BDD

```
Feature: Analyst geographic authorization

  Scenario: Analyst accesses covered state
    Given an ANALYST covers DF and GO
    And a solicitation belongs to DF
    When the analyst accesses it
    Then access should be allowed
```

```
Scenario: Analyst accesses uncovered state
  Given an ANALYST covers DF
  And a solicitation belongs to SP
  When the analyst accesses it
  Then access should be denied
```

```
Scenario: Admin accesses any state
  Given an ADMIN
  And a solicitation belongs to AM
  When the admin accesses it
  Then access should be allowed
```

Essa fase deveria ter testes unitários da policy isoladamente.

---

# Fase 10 — Workflow do Analyst — COMPLETED

## Endpoints

```
POST /analyst/solicitations/{id}/start
POST /analyst/solicitations/{id}/decide
```

Request de decisão:

```
{
  "decision": "APPROVE",
  "comment": "Solicitação validada."
}
```

## BDD

```
Feature: Analyst review workflow

  Scenario: Analyst starts review
    Given a SUBMITTED solicitation in an analyst's coverage
    When the analyst starts the review
    Then status should become IN_REVIEW
```

```
Scenario: Analyst approves solicitation
  Given an IN_REVIEW solicitation
  And the analyst covers its state
  When the analyst approves it
  Then status should become APPROVED
```

```
Scenario: Analyst rejects solicitation
  Given an IN_REVIEW solicitation
  When the analyst rejects it with a valid comment
  Then status should become REJECTED
```

```
Scenario: Analyst cannot approve outside coverage
  Given an analyst covers DF
  And a solicitation belongs to SP
  When the analyst attempts to approve it
  Then access should be denied
```

```
Scenario: Analyst cannot approve a draft
  Given a DRAFT solicitation
  When an analyst attempts to approve it
  Then the operation should be rejected
```

### Concorrência

```
Scenario: Two analysts decide simultaneously
  Given two analysts load the same IN_REVIEW solicitation
  When analyst A approves it
  And analyst B tries to reject the stale version
  Then analyst A's decision should succeed
  And analyst B should receive a conflict response
```

Esse é um excelente teste para `@Version`.

---

# Fase 11 — Elasticsearch Search Model — COMPLETED

Agora sim Elasticsearch.

## Documento

```
SolicitationSearchDocument
```

Campos:

```
id
clientId
serviceType
title
status
priority
state
preferredDate
createdAt
updatedAt
```

## Endpoint

```
GET /analyst/solicitations/search
```

Query params:

```
status
serviceType
priority
state
fromDate
toDate
page
size
sort
```

## Primeiro estágio

Nesta fase ainda pode indexar diretamente depois da alteração.

O Outbox vem depois.

## BDD

```
Feature: Solicitation search

  Scenario: Searching by status
    Given Elasticsearch contains submitted and approved solicitations
    When an analyst searches with status SUBMITTED
    Then only submitted solicitations should be returned
```

```
Scenario: Filtering by priority and state
  Given multiple indexed solicitations
  When the analyst filters by HIGH priority and DF
  Then only matching solicitations should be returned
```

```
Scenario: Paginating results
  Given 25 matching solicitations
  When page 1 with size 10 is requested
  Then 10 items should be returned
  And pagination metadata should indicate 25 total items
```

### Segurança crítica

```
Scenario: Analyst cannot bypass state coverage using search filters
  Given the analyst covers DF and GO
  And Elasticsearch contains solicitations from DF, GO and SP
  When the analyst searches with state SP
  Then no SP solicitation should be returned
```

Outro:

```
Scenario: Search filter is intersected with analyst coverage
  Given the analyst covers DF and GO
  When the analyst requests DF
  Then only DF results should be returned
```

---

# Fase 12 — Transactional Outbox — COMPLETED

Agora substituímos dual write.

## Migration

```
outbox_events
```

Campos:

```
id
aggregate_id
aggregate_type
event_type
payload
created_at
processed_at
attempts
```

## Eventos

Não criaria dezenas.

Algo simples:

```
SolicitationChanged
```

ou:

```
SOLICITATION_CREATED
SOLICITATION_UPDATED
SOLICITATION_SUBMITTED
SOLICITATION_DECIDED
```

## Fluxo

Dentro da mesma transação:

```
repository.save()
outbox.save()
COMMIT
```

Fora:

```
OutboxProcessor
↓
Elasticsearch
```

## Idempotência

Indexar pelo:

```
solicitationId
```

Uma atualização repetida substitui o documento.

## BDD

```
Feature: Reliable search indexing

  Scenario: Domain mutation creates an outbox event
    Given a solicitation exists
    When it is submitted
    Then the solicitation change should be committed
    And an outbox event should be committed in the same transaction
```

```
Scenario: Elasticsearch is temporarily unavailable
  Given Elasticsearch is unavailable
  When a solicitation is submitted
  Then the PostgreSQL transaction should still succeed
  And the outbox event should remain pending
```

```
Scenario: Pending event is retried
  Given an unprocessed outbox event exists
  And Elasticsearch becomes available
  When the outbox processor runs
  Then the Elasticsearch document should be updated
  And the event should be marked as processed
```

```
Scenario: Processing the same event more than once
  Given an already indexed solicitation event
  When the same event is processed again
  Then the final Elasticsearch document should remain correct
```

---

# Fase 13 — Auditoria via AOP — COMPLETED

## Annotation

```
@Audit(
    action = AuditAction.SOLICITATION_SUBMITTED
)
```

## Aspect

Captura:

```
actorId
action
aggregateType
aggregateId
timestamp
success
```

Eu evitaria serializar request inteiro por segurança.

## Tabela

```
audit_logs
```

## BDD

```
Feature: Audit trail

  Scenario: Auditing a submission
    Given a client owns a complete solicitation
    When the solicitation is submitted
    Then an audit record should be created
    And it should contain the client id
    And action should be SOLICITATION_SUBMITTED
```

```
Scenario: Auditing approval
  Given an analyst approves a solicitation
  Then an audit record should identify the analyst
  And the solicitation
  And the APPROVED action
```

```
Scenario: Read operations are not audited
  Given a client reads their solicitation
  Then no mutation audit entry should be created
```

---

# Fase 14 — Error contract — COMPLETED

## Objetivo

Garantir que a API seja previsível.

## Classes

```
ApiError
ApiFieldError
GlobalExceptionHandler
```

Formato:

```
{
  "code": "SOLICITATION_INCOMPLETE",
  "message": "Solicitation cannot be submitted",
  "errors": [
    {
      "field": "termsAccepted",
      "message": "must be accepted"
    }
  ],
  "timestamp": "..."
}
```

## Mapeamento

```
400 malformed request
401 unauthenticated
403 unauthorized
404 not found
409 invalid state/concurrency
422 business validation
502/503 ViaCEP unavailable
```

## BDD

```
Feature: API error contract

  Scenario: Bean validation error
    When invalid request fields are submitted
    Then the API should return 400
    And errors should identify each invalid field
```

```
Scenario: Business invariant violation
    Given an incomplete solicitation
    When submission is attempted
    Then the API should return 422
    And the response should use code SOLICITATION_INCOMPLETE
```

```
Scenario: Concurrent update
    Given the submitted entity version is stale
    When an update is attempted
    Then the API should return 409
```

---

# Fase 15 — BDD end-to-end completo — DEFERRED

Aqui eu teria pelo menos **um cenário que conta a história inteira do sistema**.

Usaria:

```
SpringBootTest
Testcontainers PostgreSQL
Testcontainers Elasticsearch
WireMock ViaCEP
MockMvc ou RestAssured
```

Eu tenderia a RestAssured porque deixa BDD API tests muito legíveis.

## Cenário principal

```
Feature: Complete solicitation lifecycle

  Scenario: Client creates a solicitation and analyst approves it

    Given a registered CLIENT
    And a registered ANALYST with coverage for DF

    When the client authenticates
    And creates a solicitation
    And fills valid basic data
    And fills an address with a valid DF CEP
    And fills valid confirmation data
    And submits the solicitation

    Then the solicitation status should be SUBMITTED

    When the analyst authenticates
    And searches for submitted solicitations in DF

    Then the created solicitation should be present

    When the analyst starts the review
    And approves the solicitation

    Then the solicitation status should be APPROVED
    And the PostgreSQL record should be APPROVED
    And the Elasticsearch projection should be APPROVED
    And the approval should be present in the audit log
```

Esse cenário sozinho valida quase toda a arquitetura.

---

# Fase 16 — Cenários BDD negativos end-to-end — DEFERRED

Também faria um segundo grupo focado em segurança.

```
Scenario: Client cannot access another client's solicitation
```

```
Scenario: Analyst cannot see solicitation outside coverage
```

```
Scenario: Analyst cannot force another state through search query
```

```
Scenario: Submitted solicitation cannot be edited
```

```
Scenario: Invalid CEP does not advance workflow
```

```
Scenario: Incomplete solicitation cannot be submitted
```

Esses testes dizem mais sobre qualidade do sistema do que 50 testes de getter.

---

# Fase 17 — OpenAPI, observabilidade e entrega — COMPLETED

## OpenAPI

Documentar:

```
auth
solicitations
analyst
admin
```

Cada erro relevante também deveria aparecer.

## Micrometer

Métricas:

```
solicitation.created
solicitation.submitted
solicitation.approved
solicitation.rejected

viacep.requests
viacep.failures

outbox.pending
outbox.failures
```

Não precisa inventar infraestrutura externa.

## README

Eu faria o README nessa ordem:

```
1. Challenge overview
2. How to run
3. How to test
4. Architecture
5. Domain model
6. Solicitation state machine
7. Multi-step workflow
8. Authentication and authorization
9. Elasticsearch strategy
10. Transactional Outbox
11. Audit strategy
12. Error contract
13. API documentation
14. Assumptions
15. Trade-offs
16. Production evolution
```

## BDD operacional final

```
Feature: Developer experience

  Scenario: Running the system from a clean machine
    Given Docker is installed
    And the repository was freshly cloned
    When docker compose up --build is executed
    Then PostgreSQL should start
    And Elasticsearch should start
    And the application should become healthy
    And migrations should be applied automatically
    And the API documentation should be accessible
```

---

# Como eu organizaria os testes

Eu separaria claramente três níveis.

```
src/test/java
│
├── unit
│   ├── solicitation
│   ├── security
│   └── user
│
├── integration
│   ├── persistence
│   ├── viacep
│   ├── elasticsearch
│   └── outbox
│
└── acceptance
    ├── SolicitationLifecycleAcceptanceTest
    ├── AuthorizationAcceptanceTest
    └── SearchAcceptanceTest
```

Ou por package da feature, dependendo do estilo adotado.

O importante é a pirâmide:

```
             /\
            /  \
           / E2E\
          /------\
         /Integration\
        /------------\
       /  Unit tests  \
      /________________\
```

Teríamos muitos testes de domínio, alguns de integração e poucos E2E realmente valiosos.

# Uma nuance importante sobre BDD

Eu **não necessariamente adicionaria Cucumber**.

BDD é uma forma de pensar comportamento, não uma dependência.

Podemos escrever:

```
@Test
void clientCannotSubmitIncompleteSolicitation() {

    // Given
    var solicitation = draftSolicitation();

    // When
    var exception = catchThrowable(
        () -> solicitation.submit(clock)
    );

    // Then
    assertThat(exception)
        .isInstanceOf(IncompleteSolicitationException.class);
}
```

E estruturar os acceptance tests com nomes:

```
@Nested
class WhenClientSubmitsSolicitation {

    @Test
    void shouldSubmitWhenAllStepsAreComplete() {}

    @Test
    void shouldRejectWhenAddressIsMissing() {}

    @Test
    void shouldRejectWhenTermsAreNotAccepted() {}
}
```

Isso mantém o código Java mais simples.

Se quisermos evidenciar BDD para o avaliador, podemos manter um documento:

```
docs/features/
```

com:

```
solicitation.feature
authorization.feature
analyst-review.feature
search.feature
```

sem obrigatoriamente executar Cucumber.

Eu provavelmente faria dessa forma.

---

## O roadmap que eu realmente seguiria

Em termos de commits/PRs, eu transformaria tudo nisso:

```
PR 01 — Bootstrap and infrastructure
PR 02 — Solicitation domain
PR 03 — PostgreSQL persistence
PR 04 — Authentication and users
PR 05 — Solicitation creation and ownership
PR 06 — Basic data step
PR 07 — Address step + ViaCEP
PR 08 — Confirmation step
PR 09 — Submission workflow
PR 10 — Analyst authorization
PR 11 — Analyst review workflow
PR 12 — Elasticsearch search
PR 13 — Transactional Outbox
PR 14 — Audit AOP
PR 15 — API error contract
PR 16 — End-to-end acceptance tests
PR 17 — Observability + OpenAPI + README
```

E cada PR teria esta definição de pronto:

```
✓ implementation
✓ unit tests
✓ integration tests where applicable
✓ BDD acceptance scenario
✓ no broken previous behavior
✓ README/ADR updated when architecture changes
```
