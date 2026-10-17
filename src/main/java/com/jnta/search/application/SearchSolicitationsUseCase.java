package com.jnta.search.application;

import com.jnta.shared.geography.BrazilianState;
import com.jnta.shared.identity.UserId;
import com.jnta.search.application.port.out.SolicitationSearchRepository;
import com.jnta.user.application.port.out.UserRepository;
import com.jnta.user.domain.Role;
import java.util.Set;

public class SearchSolicitationsUseCase {

    private final UserRepository userRepository;
    private final SolicitationSearchRepository searchRepository;

    public SearchSolicitationsUseCase(
            UserRepository userRepository,
            SolicitationSearchRepository searchRepository) {
        this.userRepository = userRepository;
        this.searchRepository = searchRepository;
    }

    public SolicitationSearchPage search(UserId actorId, SearchSolicitationsQuery query) {
        var actor = userRepository.findById(actorId).orElseThrow(SearchAccessDeniedException::new);
        if (actor.role() == Role.CLIENT) {
            throw new SearchAccessDeniedException();
        }

        Set<BrazilianState> allowedStates = searchStates(actor.role(), actor.coverage(), query.state());
        if (actor.role() == Role.ANALYST && query.state() != null && allowedStates.isEmpty()) {
            return SolicitationSearchPage.empty(query.page(), query.size());
        }

        return searchRepository.search(new SolicitationSearchCriteria(
                query.status(),
                query.serviceType(),
                query.priority(),
                allowedStates,
                query.fromDate(),
                query.toDate(),
                query.page(),
                query.size(),
                query.sortField(),
                query.sortDirection()));
    }

    private static Set<BrazilianState> searchStates(
            Role role,
            Set<BrazilianState> analystCoverage,
            BrazilianState requestedState) {
        if (role == Role.ADMIN) {
            return requestedState == null ? Set.of() : Set.of(requestedState);
        }
        if (requestedState == null) {
            return analystCoverage;
        }
        return analystCoverage.contains(requestedState) ? Set.of(requestedState) : Set.of();
    }
}
