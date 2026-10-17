package com.jnta.solicitation.adapter.in.web;

import com.jnta.solicitation.domain.confirmation.Priority;
import com.jnta.solicitation.domain.money.Currency;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateConfirmationRequest(
        @NotNull Priority priority,
        @NotNull LocalDate preferredDate,
        @NotNull @Valid EstimatedValue estimatedValue,
        @NotNull Boolean termsAccepted) {

    public record EstimatedValue(@NotNull BigDecimal amount, @NotNull Currency currency) {
    }
}
