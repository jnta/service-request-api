package com.jnta.solicitation.adapter.in.web;

import com.jnta.shared.identity.UserId;
import com.jnta.solicitation.application.DecideSolicitationUseCase;
import com.jnta.solicitation.application.StartSolicitationReviewUseCase;
import com.jnta.solicitation.domain.SolicitationId;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analyst/solicitations")
@RequiredArgsConstructor
public class AnalystSolicitationController {

    private final StartSolicitationReviewUseCase startReview;
    private final DecideSolicitationUseCase decide;
    private final AnalystReviewRequestMapper requestMapper;
    private final SolicitationResponseMapper responseMapper;

    @PostMapping("/{id}/start")
    public SolicitationResponse startReview(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        var solicitation = startReview.start(new SolicitationId(id), requester(jwt));
        return responseMapper.toResponse(solicitation);
    }

    @PostMapping("/{id}/decide")
    public SolicitationResponse decide(
            @PathVariable UUID id,
            @Valid @RequestBody DecideSolicitationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        var command = requestMapper.toCommand(request);
        var solicitation = decide.decide(new SolicitationId(id), requester(jwt), command);
        return responseMapper.toResponse(solicitation);
    }

    private static UserId requester(Jwt jwt) {
        return new UserId(UUID.fromString(jwt.getSubject()));
    }
}
