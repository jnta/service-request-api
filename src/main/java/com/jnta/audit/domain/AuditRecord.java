package com.jnta.audit.domain;

import com.jnta.shared.identity.UserId;
import java.time.Instant;
import java.util.UUID;

public record AuditRecord(
        UUID id,
        UserId actorId,
        AuditAction action,
        String aggregateType,
        UUID aggregateId,
        Instant occurredAt,
        boolean success) {
}
