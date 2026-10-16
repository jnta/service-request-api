package com.jnta.solicitation.domain.exception;

import com.jnta.solicitation.domain.validation.Violation;
import java.util.List;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public class IncompleteSolicitationException extends RuntimeException {

    private final List<Violation> violations;

    public IncompleteSolicitationException(List<Violation> violations) {
        super("Solicitation cannot be submitted while incomplete");
        this.violations = List.copyOf(violations);
    }
}
