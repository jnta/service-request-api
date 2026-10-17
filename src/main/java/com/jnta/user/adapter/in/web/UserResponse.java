package com.jnta.user.adapter.in.web;

import com.jnta.shared.geography.BrazilianState;
import com.jnta.user.domain.Role;
import java.util.Set;
import java.util.UUID;

public record UserResponse(UUID id, String name, String email, Role role, Set<BrazilianState> coverage) {
}
