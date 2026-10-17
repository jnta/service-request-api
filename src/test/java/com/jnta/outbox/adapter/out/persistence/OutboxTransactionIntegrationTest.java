package com.jnta.outbox.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnta.TestcontainersConfiguration;
import com.jnta.shared.config.ClockConfiguration;
import com.jnta.shared.identity.UserId;
import com.jnta.solicitation.adapter.out.persistence.JpaSolicitationRepositoryAdapter;
import com.jnta.solicitation.adapter.out.persistence.OutboxWritingSolicitationRepository;
import com.jnta.solicitation.application.port.out.SolicitationRepository;
import com.jnta.solicitation.application.port.out.VersionedSolicitation;
import com.jnta.solicitation.domain.Solicitation;
import com.jnta.solicitation.domain.SolicitationId;
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
    OutboxWritingSolicitationRepository.class,
    JpaOutboxEventRepositoryAdapter.class,
    ClockConfiguration.class
})
@Testcontainers
class OutboxTransactionIntegrationTest {

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
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("given a database transaction, when solicitation and outbox writes are rolled back, then both records are absent")
    void shouldRollbackSolicitationAndOutboxTogether() {
        var ownerId = new UserId(UUID.randomUUID());
        var solicitation = Solicitation.create(SolicitationId.generate(), ownerId);

        inNewTransaction(() -> {
            jdbcTemplate.update(
                    "insert into users (id, name, email, password_hash, user_role) values (?, ?, ?, ?, ?)",
                    ownerId.value(), "Transactional Client", ownerId.value() + "@example.com", "test-hash", "CLIENT");
            return null;
        });

        inRollbackTransaction(() -> {
            solicitationRepository.save(VersionedSolicitation.newEntry(solicitation));
            assertThat(countSolicitations(solicitation.id().value())).isEqualTo(1);
            assertThat(countOutboxEvents(solicitation.id().value())).isEqualTo(1);
            return null;
        });

        assertThat(countSolicitations(solicitation.id().value())).isZero();
        assertThat(countOutboxEvents(solicitation.id().value())).isZero();
    }

    private int countSolicitations(UUID id) {
        return jdbcTemplate.queryForObject("select count(*) from solicitations where id = ?", Integer.class, id);
    }

    private int countOutboxEvents(UUID id) {
        return jdbcTemplate.queryForObject("select count(*) from outbox_events where aggregate_id = ?", Integer.class, id);
    }

    private <T> T inNewTransaction(Supplier<T> action) {
        var transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transaction.execute(status -> action.get());
    }

    private <T> T inRollbackTransaction(Supplier<T> action) {
        var transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transaction.execute(status -> {
            T result = action.get();
            status.setRollbackOnly();
            return result;
        });
    }
}
