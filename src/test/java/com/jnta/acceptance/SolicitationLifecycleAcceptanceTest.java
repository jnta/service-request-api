package com.jnta.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import com.jayway.jsonpath.JsonPath;
import com.jnta.TestcontainersConfiguration;
import com.jnta.outbox.application.OutboxProcessor;
import com.jnta.search.adapter.out.elasticsearch.SolicitationSearchDocument;
import com.jnta.search.application.SearchSortDirection;
import com.jnta.search.application.SearchSortField;
import com.jnta.search.application.SolicitationSearchCriteria;
import com.jnta.search.application.port.out.SolicitationSearchRepository;
import com.jnta.shared.geography.BrazilianState;
import com.jnta.solicitation.domain.workflow.SolicitationStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.Ssl;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@ActiveProfiles("test")
@SpringBootTest(properties = {
    "security.bootstrap-admin.enabled=true",
    "security.bootstrap-admin.name=Acceptance Administrator",
    "security.bootstrap-admin.email=acceptance-admin@example.com",
    "security.bootstrap-admin.password=acceptance-admin-password",
    "outbox.processor.enabled=false"
})
@AutoConfigureMockMvc
@Testcontainers
class SolicitationLifecycleAcceptanceTest {

    private static final String ADMIN_EMAIL = "acceptance-admin@example.com";
    private static final String ADMIN_PASSWORD = "acceptance-admin-password";
    private static final String PASSWORD = "acceptance-client-password";
    private static final WireMockServer VIA_CEP = new WireMockServer(options().dynamicPort());
    private static final AtomicInteger ID_SEQUENCE = new AtomicInteger();

    static {
        VIA_CEP.start();
    }

    @Container
    @ServiceConnection
    @Ssl
    static final ElasticsearchContainer ELASTICSEARCH = new ElasticsearchContainer(
                    DockerImageName.parse(TestcontainersConfiguration.ELASTICSEARCH_IMAGE))
            .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
            .withStartupTimeout(Duration.ofMinutes(3));

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse(TestcontainersConfiguration.POSTGRES_IMAGE));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Clock clock;

    @Autowired
    private OutboxProcessor outboxProcessor;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    private SolicitationSearchRepository solicitationSearchRepository;

    @DynamicPropertySource
    static void viaCepProperties(DynamicPropertyRegistry registry) {
        registry.add("integration.viacep.base-url", VIA_CEP::baseUrl);
    }

    @BeforeEach
    void clearApplicationData() {
        VIA_CEP.resetAll();
        jdbcTemplate.update("delete from audit_logs");
        jdbcTemplate.update("delete from outbox_events");
        jdbcTemplate.update("delete from analyst_states");
        jdbcTemplate.update("delete from solicitations");
        jdbcTemplate.update("delete from users where user_role <> 'ADMIN'");
        var index = elasticsearchOperations.indexOps(SolicitationSearchDocument.class);
        if (index.exists()) {
            index.delete();
        }
    }

    @AfterAll
    static void stopViaCep() {
        VIA_CEP.stop();
    }

    @Test
    @DisplayName("given a registered client and covered analyst, when the client submits and the analyst approves, then database, search projection, and audit record agree")
    void shouldCompleteSolicitationLifecycleAndProjectApproval() throws Exception {
        var client = registerAndLogin();
        var analyst = createAnalystAndLogin(Set.of("DF"));
        var solicitationId = createSolicitation(client);
        completeBasicData(solicitationId, client.accessToken());
        completeAddress(solicitationId, client.accessToken(), "70040902", "DF");
        completeConfirmation(solicitationId, client.accessToken());

        mockMvc.perform(post("/solicitations/{id}/submit", solicitationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(client.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));

        processPendingOutboxEvents();
        mockMvc.perform(get("/analyst/solicitations/search")
                        .header(HttpHeaders.AUTHORIZATION, bearer(analyst.accessToken()))
                        .param("status", "SUBMITTED")
                        .param("state", "DF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].id").value(solicitationId));

        mockMvc.perform(post("/analyst/solicitations/{id}/start", solicitationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(analyst.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_REVIEW"));
        mockMvc.perform(post("/analyst/solicitations/{id}/decide", solicitationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(analyst.accessToken()))
                        .contentType(APPLICATION_JSON)
                        .content(decisionRequest("APPROVE", "Validated")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        processPendingOutboxEvents();

        assertThat(jdbcTemplate.queryForObject(
                "select status from solicitations where id = ?", String.class, UUID.fromString(solicitationId)))
                .isEqualTo("APPROVED");
        var searchDocument = elasticsearchOperations.get(solicitationId, SolicitationSearchDocument.class);
        assertThat(searchDocument).isNotNull();
        assertThat(searchDocument.getStatus()).isEqualTo("APPROVED");
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from audit_logs where actor_id = ? and aggregate_type = ? "
                        + "and aggregate_id = ? and action = ? and success",
                Integer.class,
                UUID.fromString(analyst.userId()),
                "Solicitation",
                UUID.fromString(solicitationId),
                "SOLICITATION_APPROVED"))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("given client A owns a solicitation, when client B reads it, then access is denied")
    void shouldDenyClientReadingAnotherClientsSolicitation() throws Exception {
        var owner = registerAndLogin();
        var otherClient = registerAndLogin();
        var solicitationId = createSolicitation(owner);

        mockMvc.perform(get("/solicitations/{id}", solicitationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherClient.accessToken())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("given an invalid bearer token, when a protected endpoint is requested, then the common unauthorized error is returned")
    void shouldReturnApiErrorEnvelopeForInvalidBearerToken() throws Exception {
        mockMvc.perform(get("/solicitations/{id}", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearer("invalid-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("given an analyst covering DF and a solicitation in SP, when the analyst reads it, then access is denied")
    void shouldDenyAnalystReadingSolicitationOutsideCoverage() throws Exception {
        var submitted = createSubmittedSolicitation("01001000", "SP");
        var analyst = createAnalystAndLogin(Set.of("DF"));

        mockMvc.perform(get("/solicitations/{id}", submitted.solicitationId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(analyst.accessToken())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("given an analyst covering DF and a solicitation in SP, when filtering search by SP, then no SP result is returned")
    void shouldNotAllowAnalystToExpandCoverageThroughSearchFilter() throws Exception {
        createSubmittedSolicitation("01001000", "SP");
        processPendingOutboxEvents();
        var analyst = createAnalystAndLogin(Set.of("DF"));

        mockMvc.perform(get("/analyst/solicitations/search")
                        .header(HttpHeaders.AUTHORIZATION, bearer(analyst.accessToken()))
                        .param("state", "SP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("given the search index does not exist, when searches run concurrently, then all searches complete")
    void shouldCreateSearchIndexWhenSearchesRunConcurrently() throws Exception {
        var criteria = new SolicitationSearchCriteria(
                SolicitationStatus.SUBMITTED,
                "INSTALLATION",
                null,
                Set.of(BrazilianState.SP),
                null,
                null,
                0,
                100,
                SearchSortField.CREATED_AT,
                SearchSortDirection.DESC);
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(8);
        try {
            var searches = IntStream.range(0, 8)
                    .mapToObj(index -> executor.submit(() -> {
                        start.await();
                        return solicitationSearchRepository.search(criteria);
                    }))
                    .toList();
            start.countDown();
            for (var search : searches) {
                assertThat(search.get().items()).isEmpty();
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("given a submitted solicitation, when the owner edits it, then the update is rejected")
    void shouldRejectEditingSubmittedSolicitation() throws Exception {
        var submitted = createSubmittedSolicitation("70040902", "DF");

        mockMvc.perform(put("/solicitations/{id}/steps/basic-data", submitted.solicitationId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(submitted.owner().accessToken()))
                        .contentType(APPLICATION_JSON)
                        .content(basicDataRequest()))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("given an invalid CEP, when the owner updates the address, then the workflow remains at ADDRESS")
    void shouldNotAdvanceAddressStepForInvalidCep() throws Exception {
        var client = registerAndLogin();
        var solicitationId = createSolicitation(client);
        completeBasicData(solicitationId, client.accessToken());
        stubUnknownPostalCode("00000000");

        mockMvc.perform(put("/solicitations/{id}/steps/address", solicitationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(client.accessToken()))
                        .contentType(APPLICATION_JSON)
                        .content(addressRequest("00000000")))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(get("/solicitations/{id}", solicitationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(client.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.currentStep").value("ADDRESS"));
    }

    @Test
    @DisplayName("given an incomplete draft, when the owner submits it, then submission fails without changing its status")
    void shouldRejectSubmittingIncompleteSolicitation() throws Exception {
        var client = registerAndLogin();
        var solicitationId = createSolicitation(client);

        mockMvc.perform(post("/solicitations/{id}/submit", solicitationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(client.accessToken())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("SOLICITATION_INCOMPLETE"))
                .andExpect(jsonPath("$.errors").isNotEmpty());
        mockMvc.perform(get("/solicitations/{id}", solicitationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(client.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    private TestIdentity registerAndLogin() throws Exception {
        String email = "client-%d@example.com".formatted(ID_SEQUENCE.incrementAndGet());
        MvcResult registration = mockMvc.perform(post("/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"Acceptance Client","email":"%s","password":"%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isCreated())
                .andReturn();
        String userId = JsonPath.read(registration.getResponse().getContentAsString(), "$.id");
        return new TestIdentity(userId, email, login(email, PASSWORD));
    }

    private TestIdentity createAnalystAndLogin(Set<String> coverage) throws Exception {
        String email = "analyst-%d@example.com".formatted(ID_SEQUENCE.incrementAndGet());
        String states = coverage.stream()
                .map(state -> "\"" + state + "\"")
                .collect(Collectors.joining(",", "[", "]"));
        MvcResult user = mockMvc.perform(post("/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(ADMIN_EMAIL, ADMIN_PASSWORD)))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"Acceptance Analyst","email":"%s","password":"%s","role":"ANALYST","coverage":%s}
                                """.formatted(email, PASSWORD, states)))
                .andExpect(status().isCreated())
                .andReturn();
        String userId = JsonPath.read(user.getResponse().getContentAsString(), "$.id");
        return new TestIdentity(userId, email, login(email, PASSWORD));
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private String createSolicitation(TestIdentity client) throws Exception {
        MvcResult result = mockMvc.perform(post("/solicitations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(client.accessToken())))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private SubmittedSolicitation createSubmittedSolicitation(String cep, String state) throws Exception {
        var client = registerAndLogin();
        var solicitationId = createSolicitation(client);
        completeBasicData(solicitationId, client.accessToken());
        completeAddress(solicitationId, client.accessToken(), cep, state);
        completeConfirmation(solicitationId, client.accessToken());
        mockMvc.perform(post("/solicitations/{id}/submit", solicitationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(client.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
        return new SubmittedSolicitation(solicitationId, client);
    }

    private void completeBasicData(String solicitationId, String accessToken) throws Exception {
        mockMvc.perform(put("/solicitations/{id}/steps/basic-data", solicitationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(APPLICATION_JSON)
                        .content(basicDataRequest()))
                .andExpect(status().isOk());
    }

    private void completeAddress(String solicitationId, String accessToken, String cep, String state)
            throws Exception {
        stubPostalCode(cep, state);
        mockMvc.perform(put("/solicitations/{id}/steps/address", solicitationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(APPLICATION_JSON)
                        .content(addressRequest(cep)))
                .andExpect(status().isOk());
    }

    private void completeConfirmation(String solicitationId, String accessToken) throws Exception {
        String preferredDate = LocalDate.now(clock).plusDays(10).toString();
        mockMvc.perform(put("/solicitations/{id}/steps/confirmation", solicitationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(APPLICATION_JSON)
                        .content(confirmationRequest(preferredDate)))
                .andExpect(status().isOk());
    }

    private void processPendingOutboxEvents() {
        var result = outboxProcessor.processPendingBatch();
        assertThat(result.failedCount()).isZero();
        elasticsearchOperations.indexOps(SolicitationSearchDocument.class).refresh();
    }

    private static void stubPostalCode(String cep, String state) {
        VIA_CEP.stubFor(WireMock.get(WireMock.urlEqualTo("/" + cep + "/json/"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"cep":"%s","logradouro":"Main Street","bairro":"Central","localidade":"Brasilia","uf":"%s"}
                                """.formatted(cep, state))));
    }

    private static void stubUnknownPostalCode(String cep) {
        VIA_CEP.stubFor(WireMock.get(WireMock.urlEqualTo("/" + cep + "/json/"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"erro\":true}")));
    }

    private static String basicDataRequest() {
        return """
                {"serviceType":"INSTALLATION","title":"Equipment installation","description":"Install equipment at the main office."}
                """;
    }

    private static String addressRequest(String cep) {
        return """
                {"cep":"%s","number":"123","complement":null}
                """.formatted(cep);
    }

    private static String confirmationRequest(String preferredDate) {
        return """
                {"priority":"LOW","preferredDate":"%s","estimatedValue":{"amount":"350.00","currency":"BRL"},"termsAccepted":true}
                """.formatted(preferredDate);
    }

    private static String decisionRequest(String decision, String comment) {
        return """
                {"decision":"%s","comment":"%s"}
                """.formatted(decision, comment);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private record TestIdentity(String userId, String email, String accessToken) {
    }

    private record SubmittedSolicitation(String solicitationId, TestIdentity owner) {
    }
}
