package com.jnta.solicitation.application.command;

import com.jnta.solicitation.domain.review.Decision;

public record DecideSolicitationCommand(Decision.Outcome decision, String comment) {
}
