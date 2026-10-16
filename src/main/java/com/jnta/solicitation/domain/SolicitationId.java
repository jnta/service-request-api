package com.jnta.solicitation.domain;

import java.util.UUID;

public record SolicitationId(UUID value) {

    public SolicitationId {
        if (value == null) {
            throw new IllegalArgumentException("Solicitation id is required");
        }
    }

    public static SolicitationId generate() {
        return new SolicitationId(UUID.randomUUID());
    }
}
