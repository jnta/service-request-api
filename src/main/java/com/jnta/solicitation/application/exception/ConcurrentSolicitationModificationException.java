package com.jnta.solicitation.application.exception;

import com.jnta.solicitation.domain.SolicitationId;

public class ConcurrentSolicitationModificationException extends RuntimeException {

    public ConcurrentSolicitationModificationException(SolicitationId id) {
        super("Solicitation was modified concurrently: " + id.value());
    }
}
