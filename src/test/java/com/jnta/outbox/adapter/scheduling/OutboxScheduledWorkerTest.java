package com.jnta.outbox.adapter.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jnta.outbox.application.OutboxBatchResult;
import com.jnta.outbox.application.OutboxProcessor;
import com.jnta.outbox.application.port.out.OutboxEventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class OutboxScheduledWorkerTest {

    @Test
    void shouldExposePendingAndFailedEventMetrics() {
        var processor = mock(OutboxProcessor.class);
        var repository = mock(OutboxEventRepository.class);
        var meterRegistry = new SimpleMeterRegistry();
        when(repository.countPending()).thenReturn(7L);
        when(processor.processPendingBatch()).thenReturn(new OutboxBatchResult(3, 2));
        var worker = new OutboxScheduledWorker(processor, repository, meterRegistry);

        worker.processPendingEvents();

        assertThat(meterRegistry.get("outbox.pending").gauge().value()).isEqualTo(7);
        assertThat(meterRegistry.get("outbox.failures").counter().count()).isEqualTo(2);
    }
}
