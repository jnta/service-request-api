package com.jnta.outbox.application;

import com.jnta.outbox.application.port.out.OutboxEventRepository;
import com.jnta.outbox.domain.OutboxEvent;
import com.jnta.solicitation.application.port.out.SolicitationRepository;
import com.jnta.solicitation.application.port.out.SolicitationSearchProjection;
import com.jnta.solicitation.domain.SolicitationId;
import java.time.Clock;

public class OutboxProcessor {

    private static final int BATCH_SIZE = 100;

    private final OutboxEventRepository outboxEventRepository;
    private final SolicitationRepository solicitationRepository;
    private final SolicitationSearchProjection searchProjection;
    private final Clock clock;

    public OutboxProcessor(
            OutboxEventRepository outboxEventRepository,
            SolicitationRepository solicitationRepository,
            SolicitationSearchProjection searchProjection,
            Clock clock) {
        this.outboxEventRepository = outboxEventRepository;
        this.solicitationRepository = solicitationRepository;
        this.searchProjection = searchProjection;
        this.clock = clock;
    }

    public OutboxBatchResult processPendingBatch() {
        int processed = 0;
        int failed = 0;
        for (OutboxEvent event : outboxEventRepository.findPending(BATCH_SIZE)) {
            try {
                process(event);
                outboxEventRepository.markProcessed(event.id(), clock.instant());
                processed++;
            } catch (RuntimeException exception) {
                outboxEventRepository.recordFailure(event.id(), exception.getClass().getSimpleName());
                failed++;
            }
        }
        return new OutboxBatchResult(processed, failed);
    }

    private void process(OutboxEvent event) {
        if (!event.aggregateType().equals("SOLICITATION") || !event.eventType().equals("SOLICITATION_CHANGED")) {
            throw new IllegalStateException("Unsupported outbox event type");
        }
        var stored = solicitationRepository.findById(new SolicitationId(event.aggregateId()))
                .orElseThrow(() -> new IllegalStateException("Outbox aggregate no longer exists"));
        searchProjection.index(stored);
    }
}
