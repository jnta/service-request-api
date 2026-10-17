package com.jnta.solicitation.application.command;

import com.jnta.solicitation.domain.confirmation.Priority;
import com.jnta.solicitation.domain.money.Currency;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateConfirmationCommand(
        Priority priority,
        LocalDate preferredDate,
        BigDecimal estimatedAmount,
        Currency currency,
        Boolean termsAccepted) {
}
