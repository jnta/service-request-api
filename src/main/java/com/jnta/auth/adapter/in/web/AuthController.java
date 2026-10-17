package com.jnta.auth.adapter.in.web;

import com.jnta.auth.application.AuthenticateUserUseCase;
import com.jnta.auth.application.RegisterClientUseCase;
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
public class AuthController {

    private final RegisterClientUseCase registerClient;
    private final AuthenticateUserUseCase authenticateUser;
    private final AuthRequestMapper mapper;

    @PostMapping("/register")
    public ResponseEntity<RegisteredClientResponse> register(@Valid @RequestBody RegisterClientRequest request) {
        var user = registerClient.register(mapper.toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(user));
    }

    @PostMapping("/login")
    public AccessTokenResponse login(@Valid @RequestBody LoginRequest request) {
        return new AccessTokenResponse(authenticateUser.authenticate(mapper.toCommand(request)), "Bearer");
    }
}
