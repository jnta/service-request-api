package com.jnta.solicitation.application.exception;

import com.jnta.solicitation.domain.SolicitationId;

public class SolicitationAccessDeniedException extends RuntimeException {

    public SolicitationAccessDeniedException(SolicitationId id) {
        super("Access to solicitation was denied: " + id.value());
    }
}
