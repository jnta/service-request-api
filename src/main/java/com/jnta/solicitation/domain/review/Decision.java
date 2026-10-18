package com.jnta.solicitation.domain.review;

import com.jnta.solicitation.domain.exception.InvalidSolicitationDataException;

public record Decision(Outcome outcome, String comment) {

    public Decision {
        if (outcome == null) {
            throw new InvalidSolicitationDataException("Decision outcome is required");
        }
        if (comment == null || comment.trim().length() < 10 || comment.trim().length() > 1000) {
            throw new InvalidSolicitationDataException("Decision comment must contain between 10 and 1000 characters");
        }
        comment = comment.trim();
    }

    public enum Outcome {
        APPROVE,
        REJECT
    }
}
