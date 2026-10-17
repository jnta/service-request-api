package com.jnta.outbox.adapter.out.persistence;

import com.jnta.outbox.application.port.out.OutboxEventRepository;
import com.jnta.outbox.domain.OutboxEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class JpaOutboxEventRepositoryAdapter implements OutboxEventRepository {

    private final OutboxEventJpaRepository repository;

    @Override
    @Transactional
    public void append(OutboxEvent event) {
        repository.saveAndFlush(toEntity(event));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutboxEvent> findPending(int limit) {
        return repository.findByProcessedAtIsNullOrderByCreatedAtAsc(PageRequest.of(0, limit))
                .stream()
                .map(JpaOutboxEventRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countPending() {
        return repository.countByProcessedAtIsNull();
    }

    @Override
    @Transactional
    public void markProcessed(UUID eventId, Instant processedAt) {
        var entity = repository.findById(eventId).orElseThrow();
        entity.processedAt = processedAt;
        entity.lastError = null;
    }

    @Override
    @Transactional
    public void recordFailure(UUID eventId, String error) {
        var entity = repository.findById(eventId).orElseThrow();
        entity.attempts++;
        entity.lastError = error == null ? "Unknown processing failure" : error.substring(0, Math.min(error.length(), 2000));
    }

    private static OutboxEventJpaEntity toEntity(OutboxEvent event) {
        var entity = new OutboxEventJpaEntity();
        entity.id = event.id();
        entity.aggregateId = event.aggregateId();
        entity.aggregateType = event.aggregateType();
        entity.eventType = event.eventType();
        entity.payload = event.payload();
        entity.createdAt = event.createdAt();
        entity.processedAt = event.processedAt();
        entity.attempts = event.attempts();
        entity.lastError = event.lastError();
        return entity;
    }

    private static OutboxEvent toDomain(OutboxEventJpaEntity entity) {
        return new OutboxEvent(
                entity.id,
                entity.aggregateId,
                entity.aggregateType,
                entity.eventType,
                entity.payload,
                entity.createdAt,
                entity.processedAt,
                entity.attempts,
                entity.lastError);
    }
}
