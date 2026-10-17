package com.jnta.search.application;

import com.jnta.shared.geography.BrazilianState;
import com.jnta.solicitation.domain.confirmation.Priority;
import com.jnta.solicitation.domain.workflow.SolicitationStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SolicitationSearchResult(
        UUID id,
        UUID clientId,
        String serviceType,
        String title,
        SolicitationStatus status,
        Priority priority,
        BrazilianState state,
        LocalDate preferredDate,
        Instant createdAt,
        Instant updatedAt) {
}
