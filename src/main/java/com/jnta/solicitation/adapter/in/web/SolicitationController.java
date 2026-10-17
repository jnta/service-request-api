package com.jnta.solicitation.adapter.in.web;

import com.jnta.shared.identity.UserId;
import com.jnta.solicitation.application.CreateSolicitationUseCase;
import com.jnta.solicitation.application.GetSolicitationUseCase;
import com.jnta.solicitation.application.UpdateBasicDataUseCase;
import com.jnta.solicitation.application.command.UpdateBasicDataCommand;
import com.jnta.solicitation.domain.SolicitationId;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/solicitations")
@RequiredArgsConstructor
public class SolicitationController {

    private final CreateSolicitationUseCase createSolicitation;
    private final GetSolicitationUseCase getSolicitation;
    private final UpdateBasicDataUseCase updateBasicData;
    private final SolicitationResponseMapper responseMapper;
    private final BasicDataRequestMapper basicDataRequestMapper;

    @PostMapping
    public ResponseEntity<SolicitationResponse> create(@AuthenticationPrincipal Jwt jwt) {
        var solicitation = createSolicitation.create(requester(jwt));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(solicitation.id().value())
                .toUri();
        return ResponseEntity.created(location).body(responseMapper.toResponse(solicitation));
    }

    @GetMapping("/{id}")
    public SolicitationResponse get(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        var solicitation = getSolicitation.get(new SolicitationId(id), requester(jwt));
        return responseMapper.toResponse(solicitation);
    }

    @PutMapping("/{id}/steps/basic-data")
    public SolicitationResponse updateBasicData(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBasicDataRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        var command = basicDataRequestMapper.toCommand(request);
        var solicitation = updateBasicData.update(new SolicitationId(id), requester(jwt), command);
        return responseMapper.toResponse(solicitation);
    }

    private static UserId requester(Jwt jwt) {
        return new UserId(UUID.fromString(jwt.getSubject()));
    }
}
