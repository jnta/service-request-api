package com.jnta.solicitation.domain.exception;

import java.util.List;

public class IncompleteSolicitationException extends RuntimeException {

    private final List<String> violations;

    public IncompleteSolicitationException(List<String> violations) {
        super("Solicitation cannot be submitted while incomplete");
        this.violations = List.copyOf(violations);
    }

    public List<String> violations() {
        return violations;
    }
}
