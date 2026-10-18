package com.jnta.search.application;

import com.jnta.shared.geography.BrazilianState;
import com.jnta.solicitation.domain.confirmation.Priority;
import com.jnta.solicitation.domain.workflow.SolicitationStatus;
import java.time.LocalDate;

public record SearchSolicitationsQuery(
        String q,
        SolicitationStatus status,
        String serviceType,
        Priority priority,
        BrazilianState state,
        LocalDate fromDate,
        LocalDate toDate,
        int page,
        int size,
        SearchSortField sortField,
        SearchSortDirection sortDirection) {

    public SearchSolicitationsQuery {
        if (page < 0 || size < 1 || size > 100) {
            throw new InvalidSearchQueryException("Page must be non-negative and size must be between 1 and 100");
        }
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new InvalidSearchQueryException("dateFrom must not be after dateTo");
        }
        if (sortField == null || sortDirection == null) {
            throw new InvalidSearchQueryException("Sort field and direction are required");
        }
        serviceType = serviceType == null || serviceType.isBlank()
                ? null
                : serviceType.trim().toUpperCase(java.util.Locale.ROOT);
        q = q == null || q.isBlank() ? null : q.trim();
    }
}
