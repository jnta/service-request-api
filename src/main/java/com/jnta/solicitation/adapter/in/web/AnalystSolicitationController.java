package com.jnta.solicitation.adapter.in.web;

import com.jnta.error.adapter.in.web.ApiError;
import com.jnta.shared.identity.UserId;
import com.jnta.solicitation.application.DecideSolicitationUseCase;
import com.jnta.solicitation.application.GetSolicitationUseCase;
import com.jnta.solicitation.application.StartSolicitationReviewUseCase;
import com.jnta.solicitation.adapter.observability.SolicitationMetrics;
import com.jnta.solicitation.domain.SolicitationId;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analyst/solicitations")
@RequiredArgsConstructor
@Tag(name = "Analyst", description = "Analyst search, review, and decision workflows")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Malformed or invalid request", content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(responseCode = "401", description = "Authentication is required", content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(responseCode = "403", description = "Analyst role or state coverage is required", content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(responseCode = "404", description = "Solicitation was not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(responseCode = "409", description = "Solicitation state or version conflict", content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
})
public class AnalystSolicitationController {

    private final StartSolicitationReviewUseCase startReview;
    private final DecideSolicitationUseCase decide;
    private final GetSolicitationUseCase getSolicitation;
    private final AnalystReviewRequestMapper requestMapper;
    private final SolicitationResponseMapper responseMapper;
    private final SolicitationMetrics metrics;

    @GetMapping("/{id}")
    @Operation(summary = "Get a solicitation for review", description = "Returns a solicitation when it is covered by the analyst's state coverage.")
    public SolicitationResponse get(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        var solicitation = getSolicitation.getForAnalyst(new SolicitationId(id), requester(jwt));
        return responseMapper.toResponse(solicitation);
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Start solicitation review", description = "Moves a covered SUBMITTED solicitation into IN_REVIEW.")
    public SolicitationResponse startReview(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        var solicitation = startReview.start(new SolicitationId(id), requester(jwt));
        return responseMapper.toResponse(solicitation);
    }

    @PostMapping("/{id}/decide")
    @Operation(summary = "Decide a solicitation", description = "Approves or rejects a solicitation in review with a comment.")
    public SolicitationResponse decide(
            @PathVariable UUID id,
            @Valid @RequestBody DecideSolicitationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        var command = requestMapper.toCommand(request);
        var solicitation = decide.decide(new SolicitationId(id), requester(jwt), command);
        metrics.decided(command.decision());
        return responseMapper.toResponse(solicitation);
    }

    private static UserId requester(Jwt jwt) {
        return new UserId(UUID.fromString(jwt.getSubject()));
    }
}
