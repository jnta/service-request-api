package com.jnta.outbox.application.port.out;

import com.jnta.outbox.domain.OutboxEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository {

    void append(OutboxEvent event);

    List<OutboxEvent> findPending(int limit);

    long countPending();

    void markProcessed(UUID eventId, Instant processedAt);

    void recordFailure(UUID eventId, String error);
}
