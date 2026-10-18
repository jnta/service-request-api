package com.jnta.solicitation.domain.confirmation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jnta.solicitation.domain.exception.InvalidSolicitationDataException;
import com.jnta.solicitation.domain.validation.Violation;
import com.jnta.solicitation.domain.money.Currency;
import com.jnta.solicitation.domain.money.Money;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConfirmationTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 16);

    @Test
    @DisplayName("given HIGH priority and an estimated value below 100, when confirmation is created, then it is rejected")
    void shouldRejectLowEstimatedValueForHighPriority() {
        assertThatThrownBy(() -> confirmation(Priority.HIGH, 9_999, true, TODAY))
                .isInstanceOf(InvalidSolicitationDataException.class);
    }

    @Test
    @DisplayName("given valid confirmation data and unaccepted terms, when created, then confirmation is rejected")
    void shouldRejectUnacceptedTerms() {
        assertThatThrownBy(() -> confirmation(Priority.LOW, 0, false, TODAY))
                .isInstanceOf(InvalidSolicitationDataException.class);
    }

    @Test
    @DisplayName("given a preferred date in the past, when submission violations are checked, then the date is reported")
    void shouldReportPastPreferredDateAtSubmission() {
        var confirmation = confirmation(Priority.LOW, 0, true, TODAY.minusDays(1));

        assertThat(confirmation.submissionViolations(TODAY))
                .containsExactly(new Violation("preferredDate", "Preferred date cannot be in the past"));
    }

    @Test
    @DisplayName("given a negative estimated value, when confirmation is created, then it is rejected")
    void shouldRejectNegativeEstimatedValue() {
        assertThatThrownBy(() -> confirmation(Priority.LOW, -1, true, TODAY))
                .isInstanceOf(InvalidSolicitationDataException.class);
    }

    private static Confirmation confirmation(Priority priority, long valueInMinorUnits, boolean termsAccepted, LocalDate date) {
        return new Confirmation(priority, date, new Money(valueInMinorUnits, Currency.BRL), termsAccepted);
    }
}
