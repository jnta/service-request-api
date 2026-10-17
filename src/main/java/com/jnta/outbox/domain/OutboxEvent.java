package com.jnta.outbox.domain;

import java.time.Instant;
import java.util.UUID;

public record OutboxEvent(
        UUID id,
        UUID aggregateId,
        String aggregateType,
        String eventType,
        String payload,
        Instant createdAt,
        Instant processedAt,
        int attempts,
        String lastError) {

    public static OutboxEvent solicitationChanged(UUID solicitationId, Instant createdAt) {
        return new OutboxEvent(
                UUID.randomUUID(),
                solicitationId,
                "SOLICITATION",
                "SOLICITATION_CHANGED",
                "{\"solicitationId\":\"" + solicitationId + "\"}",
                createdAt,
                null,
                0,
                null);
    }
}
