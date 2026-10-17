package com.jnta.search.adapter.out.elasticsearch;

import com.jnta.TestcontainersConfiguration;
import com.jnta.outbox.application.OutboxProcessor;
import com.jnta.outbox.application.port.out.OutboxEventRepository;
import com.jnta.outbox.domain.OutboxEvent;
import com.jnta.shared.geography.BrazilianState;
import com.jnta.shared.identity.UserId;
import com.jnta.auth.application.port.out.AccessTokenIssuer;
import com.jnta.solicitation.application.port.out.SolicitationRepository;
import com.jnta.solicitation.application.port.out.VersionedSolicitation;
import com.jnta.solicitation.domain.Solicitation;
import com.jnta.solicitation.domain.SolicitationId;
import com.jnta.solicitation.domain.address.Address;
import com.jnta.solicitation.domain.address.Cep;
import com.jnta.solicitation.domain.basicdata.BasicData;
import com.jnta.solicitation.domain.basicdata.ServiceType;
import com.jnta.solicitation.domain.confirmation.Confirmation;
import com.jnta.solicitation.domain.confirmation.Priority;
import com.jnta.solicitation.domain.money.Currency;
import com.jnta.solicitation.domain.money.Money;
import com.jnta.solicitation.domain.workflow.SolicitationStatus;
import com.jnta.user.application.port.out.UserRepository;
import com.jnta.user.domain.EmailAddress;
import com.jnta.user.domain.PasswordHash;
import com.jnta.user.domain.Role;
import com.jnta.user.domain.User;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.springframework.boot.testcontainers.service.connection.Ssl;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class SolicitationSearchIntegrationTest {

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
    private SolicitationRepository solicitationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    private OutboxProcessor outboxProcessor;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private Clock clock;

    @Autowired
    private AccessTokenIssuer accessTokenIssuer;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void clearState() {
        jdbcTemplate.update("delete from outbox_events");
        jdbcTemplate.update("delete from analyst_states");
        jdbcTemplate.update("delete from solicitations");
        jdbcTemplate.update("delete from users");
        var index = elasticsearchOperations.indexOps(SolicitationSearchDocument.class);
        if (index.exists()) {
            index.delete();
        }
    }

    @Test
    @DisplayName("given indexed solicitations with several statuses, when filtered by status, then only matching items are returned")
    void shouldSearchByStatus() throws Exception {
        var analyst = analystWithCoverage(Set.of(BrazilianState.DF, BrazilianState.GO));
        var submitted = createSolicitation(BrazilianState.DF, Priority.HIGH, SolicitationStatus.SUBMITTED);
        createSolicitation(BrazilianState.GO, Priority.LOW, SolicitationStatus.APPROVED);
        refreshIndex();

        mockMvc.perform(get("/analyst/solicitations/search")
                        .header(HttpHeaders.AUTHORIZATION, bearer(analyst))
                        .param("status", "SUBMITTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].id").value(submitted.id().value().toString()));
    }

    @Test
    @DisplayName("given multiple indexed solicitations, when filtered by priority and state, then only matching items are returned")
    void shouldFilterByPriorityAndState() throws Exception {
        var analyst = analystWithCoverage(Set.of(BrazilianState.DF, BrazilianState.GO));
        var matching = createSolicitation(BrazilianState.DF, Priority.HIGH, SolicitationStatus.SUBMITTED);
        createSolicitation(BrazilianState.DF, Priority.LOW, SolicitationStatus.SUBMITTED);
        createSolicitation(BrazilianState.GO, Priority.HIGH, SolicitationStatus.SUBMITTED);
        refreshIndex();

        mockMvc.perform(get("/analyst/solicitations/search")
                        .header(HttpHeaders.AUTHORIZATION, bearer(analyst))
                        .param("priority", "HIGH")
                        .param("state", "DF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(matching.id().value().toString()));
    }

    @Test
    @DisplayName("given 25 matching documents, when page one with size 10 is requested, then pagination metadata is accurate")
    void shouldPaginateResults() throws Exception {
        var analyst = analystWithCoverage(Set.of(BrazilianState.DF));
        for (int index = 0; index < 25; index++) {
            createSolicitation(BrazilianState.DF, Priority.MEDIUM, SolicitationStatus.SUBMITTED);
        }
        refreshIndex();

        mockMvc.perform(get("/analyst/solicitations/search")
                        .header(HttpHeaders.AUTHORIZATION, bearer(analyst))
                        .param("status", "SUBMITTED")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(10))
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.page").value(1));
    }

    @Test
    @DisplayName("given an analyst covering DF and GO, when state filters are applied, then search never returns SP")
    void shouldIntersectStateFilterWithAnalystCoverage() throws Exception {
        var analyst = analystWithCoverage(Set.of(BrazilianState.DF, BrazilianState.GO));
        createSolicitation(BrazilianState.DF, Priority.HIGH, SolicitationStatus.SUBMITTED);
        createSolicitation(BrazilianState.GO, Priority.HIGH, SolicitationStatus.SUBMITTED);
        createSolicitation(BrazilianState.SP, Priority.HIGH, SolicitationStatus.SUBMITTED);
        refreshIndex();

        mockMvc.perform(get("/analyst/solicitations/search")
                        .header(HttpHeaders.AUTHORIZATION, bearer(analyst))
                        .param("state", "SP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));

        mockMvc.perform(get("/analyst/solicitations/search")
                        .header(HttpHeaders.AUTHORIZATION, bearer(analyst)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[*].state").value(
                        org.hamcrest.Matchers.containsInAnyOrder("DF", "GO")));
    }

    @Test
    @DisplayName("given an already indexed solicitation event, when it is processed again, then the search document is not duplicated")
    void shouldProcessDuplicateOutboxEventIdempotently() throws Exception {
        var analyst = analystWithCoverage(Set.of(BrazilianState.DF));
        var solicitation = createSolicitation(BrazilianState.DF, Priority.MEDIUM, SolicitationStatus.SUBMITTED);
        refreshIndex();

        outboxEventRepository.append(OutboxEvent.solicitationChanged(solicitation.id().value(), clock.instant()));
        outboxProcessor.processPendingBatch();
        elasticsearchOperations.indexOps(SolicitationSearchDocument.class).refresh();

        mockMvc.perform(get("/analyst/solicitations/search")
                        .header(HttpHeaders.AUTHORIZATION, bearer(analyst))
                        .param("status", "SUBMITTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    private User analystWithCoverage(Set<BrazilianState> coverage) {
        var analyst = User.createManaged(
                UserId.generate(),
                "Search Analyst",
                new EmailAddress(UUID.randomUUID() + "@example.com"),
                new PasswordHash("test-hash"),
                Role.ANALYST,
                coverage);
        return userRepository.save(analyst);
    }

    private Solicitation createSolicitation(
            BrazilianState state, Priority priority, SolicitationStatus targetStatus) {
        var client = userRepository.save(User.registerClient(
                UserId.generate(),
                "Search Client",
                new EmailAddress(UUID.randomUUID() + "@example.com"),
                new PasswordHash("test-hash")));
        var solicitation = Solicitation.create(SolicitationId.generate(), client.id());
        solicitation.updateBasicData(new BasicData(
                new ServiceType("INSTALLATION"), "Equipment installation", "Install equipment at the main office."));
        solicitation.updateAddress(new Address(
                new Cep("70040902"), "Main Street", "Central", "Brasilia", state, "123", null));
        var value = priority == Priority.HIGH ? 10_000 : 5_000;
        solicitation.updateConfirmation(
                new Confirmation(priority, LocalDate.now(clock).plusDays(30),
                        new Money(value, Currency.BRL), true),
                clock);
        if (targetStatus == SolicitationStatus.SUBMITTED
                || targetStatus == SolicitationStatus.IN_REVIEW
                || targetStatus == SolicitationStatus.APPROVED
                || targetStatus == SolicitationStatus.REJECTED) {
            solicitation.submit(clock);
        }
        if (targetStatus == SolicitationStatus.IN_REVIEW
                || targetStatus == SolicitationStatus.APPROVED
                || targetStatus == SolicitationStatus.REJECTED) {
            solicitation.startReview();
        }
        if (targetStatus == SolicitationStatus.APPROVED) {
            solicitation.approve("Approved");
        }
        if (targetStatus == SolicitationStatus.REJECTED) {
            solicitation.reject("Rejected");
        }
        return solicitationRepository.save(VersionedSolicitation.newEntry(solicitation)).solicitation();
    }

    private void refreshIndex() {
        outboxProcessor.processPendingBatch();
        elasticsearchOperations.indexOps(SolicitationSearchDocument.class).refresh();
    }

    private String bearer(User analyst) {
        return "Bearer " + accessTokenIssuer.issue(analyst);
    }
}
