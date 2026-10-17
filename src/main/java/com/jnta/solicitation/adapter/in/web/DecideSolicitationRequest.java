package com.jnta.solicitation.adapter.in.web;

import com.jnta.solicitation.domain.review.Decision;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DecideSolicitationRequest(
        @NotNull Decision.Outcome decision,
        @NotBlank @Size(max = 1000) String comment) {
}
