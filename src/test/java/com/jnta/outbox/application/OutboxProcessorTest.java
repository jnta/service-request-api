package com.jnta.outbox.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jnta.outbox.application.port.out.OutboxEventRepository;
import com.jnta.outbox.domain.OutboxEvent;
import com.jnta.shared.identity.UserId;
import com.jnta.solicitation.application.port.out.SolicitationRepository;
import com.jnta.solicitation.application.port.out.SolicitationSearchProjection;
import com.jnta.solicitation.application.port.out.VersionedSolicitation;
import com.jnta.solicitation.domain.Solicitation;
import com.jnta.solicitation.domain.SolicitationId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxProcessorTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-16T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private SolicitationRepository solicitationRepository;

    @Mock
    private SolicitationSearchProjection searchProjection;

    private OutboxProcessor outboxProcessor;

    @BeforeEach
    void createProcessor() {
        outboxProcessor = new OutboxProcessor(
                outboxEventRepository, solicitationRepository, searchProjection, CLOCK);
    }

    @Test
    @DisplayName("given Elasticsearch fails once, when a pending event is retried, then it is eventually marked processed")
    void shouldRetryFailedProjection() {
        var id = SolicitationId.generate();
        var solicitation = Solicitation.create(id, new UserId(UUID.randomUUID()));
        var stored = VersionedSolicitation.persisted(solicitation, 0, CLOCK.instant(), CLOCK.instant());
        var event = OutboxEvent.solicitationChanged(id.value(), CLOCK.instant());
        when(outboxEventRepository.findPending(anyInt())).thenReturn(List.of(event), List.of(event));
        when(solicitationRepository.findById(id)).thenReturn(Optional.of(stored));
        doThrow(new IllegalStateException("Elasticsearch unavailable"))
                .doNothing()
                .when(searchProjection)
                .index(stored);

        assertThat(outboxProcessor.processPendingBatch())
                .isEqualTo(new OutboxBatchResult(0, 1));
        verify(outboxEventRepository).recordFailure(event.id(), "IllegalStateException");

        assertThat(outboxProcessor.processPendingBatch())
                .isEqualTo(new OutboxBatchResult(1, 0));
        verify(outboxEventRepository).markProcessed(event.id(), CLOCK.instant());
        verify(searchProjection, times(2)).index(stored);
    }
}
