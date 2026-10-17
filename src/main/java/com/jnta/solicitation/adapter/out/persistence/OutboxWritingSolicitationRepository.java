package com.jnta.solicitation.adapter.out.persistence;

import com.jnta.outbox.application.port.out.OutboxEventRepository;
import com.jnta.outbox.domain.OutboxEvent;
import com.jnta.solicitation.application.port.out.SolicitationRepository;
import com.jnta.solicitation.application.port.out.VersionedSolicitation;
import com.jnta.solicitation.domain.SolicitationId;
import java.time.Clock;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Primary
public class OutboxWritingSolicitationRepository implements SolicitationRepository {

    private final SolicitationRepository persistenceRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final Clock clock;

    public OutboxWritingSolicitationRepository(
            @Qualifier("jpaSolicitationRepositoryAdapter") SolicitationRepository persistenceRepository,
            OutboxEventRepository outboxEventRepository,
            Clock clock) {
        this.persistenceRepository = persistenceRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public VersionedSolicitation save(VersionedSolicitation solicitation) {
        var stored = persistenceRepository.save(solicitation);
        outboxEventRepository.append(OutboxEvent.solicitationChanged(stored.solicitation().id().value(), clock.instant()));
        return stored;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VersionedSolicitation> findById(SolicitationId id) {
        return persistenceRepository.findById(id);
    }
}
