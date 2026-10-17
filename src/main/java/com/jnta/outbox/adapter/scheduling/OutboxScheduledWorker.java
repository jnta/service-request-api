package com.jnta.outbox.adapter.scheduling;

import com.jnta.outbox.application.OutboxProcessor;
import com.jnta.outbox.application.port.out.OutboxEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxScheduledWorker {

    private final OutboxProcessor outboxProcessor;
    private final Counter failures;

    public OutboxScheduledWorker(
            OutboxProcessor outboxProcessor,
            OutboxEventRepository outboxEventRepository,
            MeterRegistry meterRegistry) {
        this.outboxProcessor = outboxProcessor;
        failures = meterRegistry.counter("outbox.failures");
        Gauge.builder("outbox.pending", outboxEventRepository, OutboxEventRepository::countPending)
                .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${outbox.processor.fixed-delay-ms:2000}")
    public void processPendingEvents() {
        var result = outboxProcessor.processPendingBatch();
        if (result.failedCount() > 0) {
            failures.increment(result.failedCount());
        }
    }
}
