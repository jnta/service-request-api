package com.jnta.user.adapter.in.web;

import com.jnta.shared.geography.BrazilianState;
import com.jnta.user.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record CreateUserRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull Role role,
        Set<BrazilianState> coverage) {

    public CreateUserRequest {
        coverage = coverage == null ? Set.of() : Set.copyOf(coverage);
    }
}
