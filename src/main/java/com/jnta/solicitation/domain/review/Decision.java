package com.jnta.solicitation.domain.review;

import com.jnta.solicitation.domain.exception.InvalidSolicitationDataException;

public record Decision(Outcome outcome, String comment) {

    public Decision {
        if (outcome == null) {
            throw new InvalidSolicitationDataException("Decision outcome is required");
        }
        comment = comment == null || comment.isBlank() ? null : comment.trim();
    }

    public enum Outcome {
        APPROVE,
        REJECT
    }
}
