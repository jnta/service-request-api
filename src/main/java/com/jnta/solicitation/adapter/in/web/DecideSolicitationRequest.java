package com.jnta.solicitation.adapter.in.web;

import com.jnta.solicitation.domain.review.Decision;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DecideSolicitationRequest(
        @NotNull Decision.Outcome decision,
        @NotBlank String comment) {
}
