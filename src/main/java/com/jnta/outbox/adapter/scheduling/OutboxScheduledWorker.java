package com.jnta.outbox.adapter.scheduling;

import com.jnta.outbox.application.OutboxProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxScheduledWorker {

    private final OutboxProcessor outboxProcessor;

    @Scheduled(fixedDelayString = "${outbox.processor.fixed-delay-ms:2000}")
    public void processPendingEvents() {
        outboxProcessor.processPendingBatch();
    }
}
