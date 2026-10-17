package com.jnta.user.application.command;

import com.jnta.shared.geography.BrazilianState;
import com.jnta.user.domain.Role;
import java.util.Set;

public record CreateManagedUserCommand(
        String name,
        String email,
        String password,
        Role role,
        Set<BrazilianState> coverage) {
}
