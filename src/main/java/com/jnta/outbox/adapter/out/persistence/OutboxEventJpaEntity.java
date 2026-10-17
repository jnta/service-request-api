package com.jnta.outbox.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "outbox_events")
public class OutboxEventJpaEntity {

    @Id
    UUID id;

    @Column(name = "aggregate_id", nullable = false)
    UUID aggregateId;

    @Column(name = "aggregate_type", nullable = false, length = 80)
    String aggregateType;

    @Column(name = "event_type", nullable = false, length = 100)
    String eventType;

    @Column(nullable = false, columnDefinition = "text")
    String payload;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "processed_at")
    Instant processedAt;

    @Column(nullable = false)
    int attempts;

    @Column(name = "last_error", columnDefinition = "text")
    String lastError;

    protected OutboxEventJpaEntity() {
    }
}
