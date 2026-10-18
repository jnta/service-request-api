package com.jnta.solicitation.domain.confirmation;

import com.jnta.solicitation.domain.exception.InvalidSolicitationDataException;
import com.jnta.solicitation.domain.validation.Violation;
import com.jnta.solicitation.domain.money.Currency;
import com.jnta.solicitation.domain.money.Money;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public record Confirmation(
        Priority priority,
        LocalDate preferredDate,
        Money estimatedValue,
        boolean termsAccepted) {

    private static final Money HIGH_PRIORITY_MINIMUM_VALUE = new Money(10_000, Currency.BRL);

    public Confirmation {
        if (priority == null) {
            throw new InvalidSolicitationDataException("Priority is required");
        }
        if (preferredDate == null) {
            throw new InvalidSolicitationDataException("Preferred date is required");
        }
        if (estimatedValue == null) {
            throw new InvalidSolicitationDataException("Estimated value is required");
        }
        if (!termsAccepted) {
            throw new InvalidSolicitationDataException("Terms must be accepted");
        }
        if (priority == Priority.HIGH && estimatedValue.isLessThan(HIGH_PRIORITY_MINIMUM_VALUE)) {
            throw new InvalidSolicitationDataException("High priority requires an estimated value of at least 100");
        }
    }

    public List<Violation> submissionViolations(LocalDate today) {
        var violations = new ArrayList<Violation>();
        if (preferredDate.isBefore(today)) {
            violations.add(new Violation("preferredDate", "Preferred date cannot be in the past"));
        }
        if (!termsAccepted) {
            violations.add(new Violation("termsAccepted", "Terms must be accepted"));
        }
        return List.copyOf(violations);
    }
}
