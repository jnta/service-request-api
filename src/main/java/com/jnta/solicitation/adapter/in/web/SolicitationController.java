package com.jnta.solicitation.adapter.in.web;

import com.jnta.error.adapter.in.web.ApiError;
import com.jnta.shared.identity.UserId;
import com.jnta.solicitation.application.CreateSolicitationUseCase;
import com.jnta.solicitation.application.GetSolicitationUseCase;
import com.jnta.solicitation.application.SubmitSolicitationUseCase;
import com.jnta.solicitation.application.UpdateAddressUseCase;
import com.jnta.solicitation.application.UpdateBasicDataUseCase;
import com.jnta.solicitation.application.UpdateConfirmationUseCase;
import com.jnta.solicitation.application.command.UpdateAddressCommand;
import com.jnta.solicitation.application.command.UpdateConfirmationCommand;
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
@Tag(name = "Solicitations", description = "Client solicitation lifecycle and step updates")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Malformed or invalid request", content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(responseCode = "401", description = "Authentication is required", content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(responseCode = "403", description = "Access is forbidden", content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(responseCode = "404", description = "Solicitation was not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(responseCode = "409", description = "Solicitation state or version conflict", content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(responseCode = "422", description = "Business validation failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(responseCode = "503", description = "ViaCEP is unavailable", content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
})
public class SolicitationController {

    private final CreateSolicitationUseCase createSolicitation;
    private final GetSolicitationUseCase getSolicitation;
    private final UpdateAddressUseCase updateAddress;
    private final UpdateBasicDataUseCase updateBasicData;
    private final UpdateConfirmationUseCase updateConfirmation;
    private final SubmitSolicitationUseCase submitSolicitation;
    private final SolicitationResponseMapper responseMapper;
    private final AddressRequestMapper addressRequestMapper;
    private final BasicDataRequestMapper basicDataRequestMapper;
    private final ConfirmationRequestMapper confirmationRequestMapper;
    private final SolicitationMetrics metrics;

    @PostMapping
    @Operation(summary = "Create a draft solicitation", description = "Creates a DRAFT owned by the authenticated client.")
    public ResponseEntity<SolicitationResponse> create(@AuthenticationPrincipal Jwt jwt) {
        var solicitation = createSolicitation.create(requester(jwt));
        metrics.created();
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(solicitation.id().value())
                .toUri();
        return ResponseEntity.created(location).body(responseMapper.toResponse(solicitation));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a solicitation", description = "Returns a solicitation when the authenticated user is authorized to read it.")
    public SolicitationResponse get(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        var solicitation = getSolicitation.get(new SolicitationId(id), requester(jwt));
        return responseMapper.toResponse(solicitation);
    }

    @PutMapping("/{id}/steps/address")
    @Operation(summary = "Update the address step", description = "Resolves address details from ViaCEP using the provided CEP.")
    public SolicitationResponse updateAddress(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAddressRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UpdateAddressCommand command = addressRequestMapper.toCommand(request);
        var solicitation = updateAddress.update(new SolicitationId(id), requester(jwt), command);
        return responseMapper.toResponse(solicitation);
    }

    @PutMapping("/{id}/steps/confirmation")
    @Operation(summary = "Update the confirmation step", description = "Stores priority, preferred date, estimated value, and terms acceptance.")
    public SolicitationResponse updateConfirmation(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateConfirmationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UpdateConfirmationCommand command = confirmationRequestMapper.toCommand(request);
        var solicitation = updateConfirmation.update(new SolicitationId(id), requester(jwt), command);
        return responseMapper.toResponse(solicitation);
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit a solicitation", description = "Submits a complete draft with accepted terms.")
    public SolicitationResponse submit(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        var solicitation = submitSolicitation.submit(new SolicitationId(id), requester(jwt));
        metrics.submitted();
        return responseMapper.toResponse(solicitation);
    }

    @PutMapping("/{id}/steps/basic-data")
    @Operation(summary = "Update basic data", description = "Stores the service type, title, and description of a draft.")
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
