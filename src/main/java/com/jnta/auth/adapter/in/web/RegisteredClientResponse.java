package com.jnta.auth.adapter.in.web;

import com.jnta.user.domain.Role;
import java.util.UUID;

public record RegisteredClientResponse(UUID id, Role role) {
}
