package com.jnta.solicitation.domain.confirmation;

import com.jnta.solicitation.domain.exception.InvalidSolicitationDataException;
import com.jnta.solicitation.domain.validation.Violation;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public record Confirmation(
        Priority priority,
        LocalDate preferredDate,
        BigDecimal estimatedValue,
        boolean termsAccepted) {

    private static final BigDecimal HIGH_PRIORITY_MINIMUM_VALUE = new BigDecimal("100");

    public Confirmation {
        if (priority == null) {
            throw new InvalidSolicitationDataException("Priority is required");
        }
        if (preferredDate == null) {
            throw new InvalidSolicitationDataException("Preferred date is required");
        }
        if (estimatedValue == null || estimatedValue.signum() < 0) {
            throw new InvalidSolicitationDataException("Estimated value must not be negative");
        }
        if (priority == Priority.HIGH && estimatedValue.compareTo(HIGH_PRIORITY_MINIMUM_VALUE) < 0) {
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
