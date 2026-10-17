package com.jnta.audit.adapter.out.persistence;

import com.jnta.audit.domain.AuditAction;
import com.jnta.audit.domain.AuditRecord;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLogJpaEntity {

    @Id
    UUID id;

    @Column(name = "actor_id", nullable = false)
    UUID actorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 80)
    AuditAction action;

    @Column(name = "aggregate_type", nullable = false, length = 80)
    String aggregateType;

    @Column(name = "aggregate_id")
    UUID aggregateId;

    @Column(name = "occurred_at", nullable = false)
    Instant occurredAt;

    @Column(nullable = false)
    boolean success;

    protected AuditLogJpaEntity() {
    }

    static AuditLogJpaEntity from(AuditRecord record) {
        var entity = new AuditLogJpaEntity();
        entity.id = record.id();
        entity.actorId = record.actorId().value();
        entity.action = record.action();
        entity.aggregateType = record.aggregateType();
        entity.aggregateId = record.aggregateId();
        entity.occurredAt = record.occurredAt();
        entity.success = record.success();
        return entity;
    }
}
