package com.jnta.solicitation.domain;

import com.jnta.solicitation.domain.exception.InvalidSolicitationDataException;
import java.util.UUID;

public record SolicitationId(UUID value) {

    public SolicitationId {
        if (value == null) {
            throw new InvalidSolicitationDataException("Solicitation id is required");
        }
    }

    public static SolicitationId generate() {
        return new SolicitationId(UUID.randomUUID());
    }
}
