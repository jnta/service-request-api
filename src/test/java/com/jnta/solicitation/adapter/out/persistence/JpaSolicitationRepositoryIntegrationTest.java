package com.jnta.solicitation.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jnta.TestcontainersConfiguration;
import com.jnta.outbox.adapter.out.persistence.JpaOutboxEventRepositoryAdapter;
import com.jnta.shared.config.ClockConfiguration;
import com.jnta.shared.identity.UserId;
import com.jnta.solicitation.application.exception.ConcurrentSolicitationModificationException;
import com.jnta.solicitation.application.port.out.SolicitationRepository;
import com.jnta.solicitation.application.port.out.VersionedSolicitation;
import com.jnta.solicitation.domain.Solicitation;
import com.jnta.solicitation.domain.SolicitationId;
import com.jnta.solicitation.domain.address.Address;
import com.jnta.solicitation.domain.address.Cep;
import com.jnta.shared.geography.BrazilianState;
import com.jnta.solicitation.domain.basicdata.BasicData;
import com.jnta.solicitation.domain.basicdata.ServiceType;
import com.jnta.solicitation.domain.confirmation.Confirmation;
import com.jnta.solicitation.domain.confirmation.Priority;
import com.jnta.solicitation.domain.money.Currency;
import com.jnta.solicitation.domain.money.Money;
import com.jnta.solicitation.domain.workflow.SolicitationStatus;
import com.jnta.solicitation.domain.workflow.SolicitationStep;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    JpaSolicitationRepositoryAdapter.class,
    JpaOutboxEventRepositoryAdapter.class,
    OutboxWritingSolicitationRepository.class,
    ClockConfiguration.class
})
@Testcontainers
class JpaSolicitationRepositoryIntegrationTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-16T12:00:00Z"), ZoneOffset.UTC);

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse(TestcontainersConfiguration.POSTGRES_IMAGE));

    @Autowired
    private SolicitationRepository solicitationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("given a new solicitation, when saved, then its search projection event is committed")
    void shouldSaveOutboxEventWithSolicitation() {
        var solicitation = Solicitation.create(SolicitationId.generate(), createClient());

        solicitationRepository.save(VersionedSolicitation.newEntry(solicitation));

        Integer eventCount = jdbcTemplate.queryForObject(
                "select count(*) from outbox_events where aggregate_id = ? and processed_at is null",
                Integer.class,
                solicitation.id().value());
        assertThat(eventCount).isEqualTo(1);
    }

    @Test
    @DisplayName("given a new draft, when persisted and loaded, then its status and first step are preserved")
    void shouldPersistDraftSolicitation() {
        var owner = createClient();
        var draft = Solicitation.create(SolicitationId.generate(), owner);

        var saved = solicitationRepository.save(VersionedSolicitation.newEntry(draft));
        var loaded = solicitationRepository.findById(draft.id()).orElseThrow();

        assertThat(loaded.solicitation().status()).isEqualTo(SolicitationStatus.DRAFT);
        assertThat(loaded.solicitation().currentStep()).isEqualTo(SolicitationStep.BASIC_DATA);
        assertThat(loaded.solicitation().owner()).isEqualTo(owner);
        assertThat(loaded.version()).isEqualTo(saved.version());
    }

    @Test
    @DisplayName("given completed basic data, when persisted and loaded, then the data is preserved and the next step is ADDRESS")
    void shouldPersistPartiallyCompletedWorkflow() {
        var solicitation = Solicitation.create(SolicitationId.generate(), createClient());
        solicitation.updateBasicData(basicData("Equipment installation"));

        solicitationRepository.save(VersionedSolicitation.newEntry(solicitation));
        var loaded = solicitationRepository.findById(solicitation.id()).orElseThrow().solicitation();

        assertThat(loaded.basicData()).isEqualTo(solicitation.basicData());
        assertThat(loaded.currentStep()).isEqualTo(SolicitationStep.ADDRESS);
    }

    @Test
    @DisplayName("given a completed draft, when persisted and loaded, then its address and confirmation are preserved")
    void shouldPersistAddressAndConfirmation() {
        var solicitation = Solicitation.create(SolicitationId.generate(), createClient());
        solicitation.updateBasicData(basicData("Equipment installation"));
        solicitation.updateAddress(address());
        solicitation.updateConfirmation(confirmation(), CLOCK);

        solicitationRepository.save(VersionedSolicitation.newEntry(solicitation));
        var loaded = solicitationRepository.findById(solicitation.id()).orElseThrow().solicitation();

        assertThat(loaded.address()).isEqualTo(solicitation.address());
        assertThat(loaded.confirmation()).isEqualTo(solicitation.confirmation());
        assertThat(loaded.hasCompletedSteps()).isTrue();
    }

    @Test
    @DisplayName("given an approved solicitation, when persisted and loaded, then its terminal status and decision are preserved")
    void shouldPersistDecision() {
        var solicitation = Solicitation.create(SolicitationId.generate(), createClient());
        solicitation.updateBasicData(basicData("Equipment installation"));
        solicitation.updateAddress(address());
        solicitation.updateConfirmation(confirmation(), CLOCK);
        solicitation.submit(CLOCK);
        solicitation.startReview();
        solicitation.approve("Validated");

        solicitationRepository.save(VersionedSolicitation.newEntry(solicitation));
        var loaded = solicitationRepository.findById(solicitation.id()).orElseThrow().solicitation();

        assertThat(loaded.status()).isEqualTo(SolicitationStatus.APPROVED);
        assertThat(loaded.decision()).isEqualTo(solicitation.decision());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("given two copies of one version, when one is saved, then the stale save fails")
    void shouldRejectStaleConcurrentModification() {
        var owner = createClientInTransaction();
        var draft = Solicitation.create(SolicitationId.generate(), owner);
        inTransaction(() -> solicitationRepository.save(VersionedSolicitation.newEntry(draft)));
        var firstCopy = inTransaction(() -> solicitationRepository.findById(draft.id()).orElseThrow());
        var staleCopy = inTransaction(() -> solicitationRepository.findById(draft.id()).orElseThrow());

        firstCopy.solicitation().updateBasicData(basicData("First update"));
        staleCopy.solicitation().updateBasicData(basicData("Stale update"));
        var updated = inTransaction(() -> solicitationRepository.save(firstCopy));

        assertThat(updated.version().getAsLong()).isGreaterThan(firstCopy.version().getAsLong());
        assertThatThrownBy(() -> inTransaction(() -> solicitationRepository.save(staleCopy)))
                .isInstanceOf(ConcurrentSolicitationModificationException.class);

        var persisted = inTransaction(() -> solicitationRepository.findById(draft.id()).orElseThrow());
        assertThat(persisted.solicitation().basicData().title()).isEqualTo("First update");
        assertThat(persisted.version()).isEqualTo(updated.version());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("given an outbox write failure, when a solicitation transaction rolls back, then neither record is persisted")
    void shouldRollbackSolicitationAndOutboxTogether() {
        var owner = createClientInTransaction();
        var solicitation = Solicitation.create(SolicitationId.generate(), owner);
        var transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transaction.execute(status -> {
            solicitationRepository.save(VersionedSolicitation.newEntry(solicitation));
            status.setRollbackOnly();
            return null;
        });

        Integer solicitationCount = jdbcTemplate.queryForObject(
                "select count(*) from solicitations where id = ?", Integer.class, solicitation.id().value());
        Integer eventCount = jdbcTemplate.queryForObject(
                "select count(*) from outbox_events where aggregate_id = ?", Integer.class, solicitation.id().value());
        assertThat(solicitationCount).isZero();
        assertThat(eventCount).isZero();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("given two analysts load one IN_REVIEW version, when one decides, then the stale decision conflicts")
    void shouldRejectStaleConcurrentDecision() {
        var owner = createClientInTransaction();
        var solicitation = Solicitation.create(SolicitationId.generate(), owner);
        solicitation.updateBasicData(basicData("Equipment installation"));
        solicitation.updateAddress(address());
        solicitation.updateConfirmation(confirmation(), CLOCK);
        solicitation.submit(CLOCK);
        solicitation.startReview();
        inTransaction(() -> solicitationRepository.save(VersionedSolicitation.newEntry(solicitation)));

        var analystAView = inTransaction(() -> solicitationRepository.findById(solicitation.id()).orElseThrow());
        var analystBView = inTransaction(() -> solicitationRepository.findById(solicitation.id()).orElseThrow());
        analystAView.solicitation().approve("Approved by analyst A");
        analystBView.solicitation().reject("Rejected by analyst B");
        inTransaction(() -> solicitationRepository.save(analystAView));

        assertThatThrownBy(() -> inTransaction(() -> solicitationRepository.save(analystBView)))
                .isInstanceOf(ConcurrentSolicitationModificationException.class);

        var persisted = inTransaction(() -> solicitationRepository.findById(solicitation.id()).orElseThrow());
        assertThat(persisted.solicitation().status()).isEqualTo(SolicitationStatus.APPROVED);
        assertThat(persisted.solicitation().decision().comment()).isEqualTo("Approved by analyst A");
    }

    private UserId createClient() {
        var id = new UserId(UUID.randomUUID());
        insertClient(id);
        return id;
    }

    private UserId createClientInTransaction() {
        var id = new UserId(UUID.randomUUID());
        inTransaction(() -> {
            insertClient(id);
            return null;
        });
        return id;
    }

    private void insertClient(UserId id) {
        jdbcTemplate.update(
                "insert into users (id, name, email, password_hash, user_role) values (?, ?, ?, ?, ?)",
                id.value(), "Test Client", id.value() + "@example.com", "test-hash", "CLIENT");
    }

    private <T> T inTransaction(Supplier<T> action) {
        var transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transaction.execute(status -> action.get());
    }

    private static BasicData basicData(String title) {
        return new BasicData(new ServiceType("INSTALLATION"), title, "Install equipment at the main office.");
    }

    private static Address address() {
        return new Address(new Cep("70040-902"), "Main Street", "Central", "Brasilia", BrazilianState.DF, "123", null);
    }

    private static Confirmation confirmation() {
        return new Confirmation(Priority.HIGH, LocalDate.of(2026, 10, 16), new Money(10_000, Currency.BRL), true);
    }
}
