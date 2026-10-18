package com.jnta.search.adapter.in.web;

import com.jnta.error.adapter.in.web.ApiError;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analyst/solicitations/search")
@RequiredArgsConstructor
@Tag(name = "Analyst", description = "Analyst search, review, and decision workflows")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Search parameters are invalid", content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(responseCode = "401", description = "Authentication is required", content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(responseCode = "403", description = "Analyst role is required", content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
})
public class SearchSolicitationsController {

    private final SearchSolicitationsUseCase searchSolicitations;

    @GetMapping
    @Operation(summary = "Search solicitations", description = "Filters the search projection. Analyst state coverage is always enforced in addition to the requested state filter.")
    public SolicitationSearchPage search(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Search text in title and description")
                    @RequestParam(required = false) String q,
            @Parameter(description = "Filter by workflow status")
                    @RequestParam(required = false) SolicitationStatus status,
            @Parameter(description = "Filter by service type")
                    @RequestParam(required = false) String serviceType,
            @Parameter(description = "Filter by priority")
                    @RequestParam(required = false) Priority priority,
            @Parameter(description = "Filter by state; intersected with analyst coverage")
                    @RequestParam(required = false) BrazilianState state,
            @Parameter(description = "Include solicitations preferred on or after this date", example = "2026-01-01")
                    @RequestParam(required = false) LocalDate dateFrom,
            @Parameter(description = "Include solicitations preferred on or before this date", example = "2026-12-31")
                    @RequestParam(required = false) LocalDate dateTo,
            @Parameter(description = "Zero-based page index", example = "0")
                    @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of results per page, from 1 through 100", example = "20")
                    @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort property and direction", example = "createdAt,desc")
                    @RequestParam(defaultValue = "createdAt,desc") String sort) {
        var sortOptions = parseSort(sort);
        var query = new SearchSolicitationsQuery(
                q,
                status,
                serviceType,
                priority,
                state,
                dateFrom,
                dateTo,
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
