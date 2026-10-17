package com.jnta.search.adapter.in.web;

import com.jnta.search.application.InvalidSearchQueryException;
import com.jnta.search.application.SolicitationSearchPage;
import com.jnta.search.application.SearchSolicitationsQuery;
import com.jnta.search.application.SearchSolicitationsUseCase;
import com.jnta.search.application.SearchSortDirection;
import com.jnta.search.application.SearchSortField;
import com.jnta.shared.geography.BrazilianState;
import com.jnta.shared.identity.UserId;
import com.jnta.solicitation.domain.confirmation.Priority;
import com.jnta.solicitation.domain.workflow.SolicitationStatus;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analyst/solicitations/search")
@RequiredArgsConstructor
public class SearchSolicitationsController {

    private final SearchSolicitationsUseCase searchSolicitations;

    @GetMapping
    public SolicitationSearchPage search(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) SolicitationStatus status,
            @RequestParam(required = false) String serviceType,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) BrazilianState state,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        var sortOptions = parseSort(sort);
        var query = new SearchSolicitationsQuery(
                status,
                serviceType,
                priority,
                state,
                fromDate,
                toDate,
                page,
                size,
                sortOptions.field(),
                sortOptions.direction());
        return searchSolicitations.search(new UserId(UUID.fromString(jwt.getSubject())), query);
    }

    private static SortOptions parseSort(String value) {
        String[] parts = value.split(",", -1);
        if (parts.length != 2) {
            throw new InvalidSearchQueryException("sort must use field,direction format");
        }
        try {
            return new SortOptions(
                    SearchSortField.fromProperty(parts[0]),
                    SearchSortDirection.valueOf(parts[1].toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            throw new InvalidSearchQueryException("sort field or direction is unsupported");
        }
    }

    private record SortOptions(SearchSortField field, SearchSortDirection direction) {
    }
}
