package com.jnta.search.application;

import com.jnta.shared.geography.BrazilianState;
import com.jnta.solicitation.domain.confirmation.Priority;
import com.jnta.solicitation.domain.workflow.SolicitationStatus;
import java.time.LocalDate;
import java.util.Set;

public record SolicitationSearchCriteria(
        String q,
        SolicitationStatus status,
        String serviceType,
        Priority priority,
        Set<BrazilianState> allowedStates,
        LocalDate fromDate,
        LocalDate toDate,
        int page,
        int size,
        SearchSortField sortField,
        SearchSortDirection sortDirection) {

    public SolicitationSearchCriteria {
        allowedStates = Set.copyOf(allowedStates);
    }
}
