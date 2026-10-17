package com.jnta.solicitation.application.exception;

import com.jnta.solicitation.domain.SolicitationId;

public class SolicitationNotFoundException extends RuntimeException {

    public SolicitationNotFoundException(SolicitationId id) {
        super("Solicitation was not found: " + id.value());
    }
}
