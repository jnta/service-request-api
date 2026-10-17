package com.jnta.solicitation.adapter.in.web;

import com.jnta.shared.geography.BrazilianState;
import com.jnta.solicitation.domain.confirmation.Priority;
import com.jnta.solicitation.domain.money.Currency;
import com.jnta.solicitation.domain.review.Decision;
import com.jnta.solicitation.domain.workflow.SolicitationStatus;
import com.jnta.solicitation.domain.workflow.SolicitationStep;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SolicitationResponse(
        UUID id,
        UUID ownerId,
        SolicitationStatus status,
        SolicitationStep currentStep,
        BasicData basicData,
        Address address,
        Confirmation confirmation,
        DecisionDetails decision) {

    public record BasicData(String serviceType, String title, String description) {
    }

    public record Address(
            String cep,
            String street,
            String neighborhood,
            String city,
            BrazilianState state,
            String number,
            String complement) {
    }

    public record Confirmation(
            Priority priority,
            LocalDate preferredDate,
            Money estimatedValue,
            boolean termsAccepted) {
    }

    public record Money(BigDecimal amount, Currency currency) {
    }

    public record DecisionDetails(Decision.Outcome outcome, String comment) {
    }
}
