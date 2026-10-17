package com.jnta.auth.adapter.in.web;

import com.jnta.auth.application.AuthenticateUserUseCase;
import com.jnta.auth.application.RegisterClientUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.jnta.error.adapter.in.web.ApiError;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Client registration and authentication")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Malformed or invalid request", content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(responseCode = "409", description = "Email is already registered", content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
})
public class AuthController {

    private final RegisterClientUseCase registerClient;
    private final AuthenticateUserUseCase authenticateUser;
    private final AuthRequestMapper mapper;

    @PostMapping("/register")
    @Operation(summary = "Register a client", description = "Creates a CLIENT account using the supplied name, email, and password.")
    public ResponseEntity<RegisteredClientResponse> register(@Valid @RequestBody RegisterClientRequest request) {
        var user = registerClient.register(mapper.toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(user));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate a user", description = "Returns a bearer JWT for valid credentials.")
    public AccessTokenResponse login(@Valid @RequestBody LoginRequest request) {
        return new AccessTokenResponse(authenticateUser.authenticate(mapper.toCommand(request)), "Bearer");
    }
}
