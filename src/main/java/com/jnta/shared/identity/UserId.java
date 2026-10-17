package com.jnta.shared.identity;

import java.util.UUID;

public record UserId(UUID value) {

    public UserId {
        if (value == null) {
            throw new IllegalArgumentException("User id is required");
        }
    }

    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }
}
