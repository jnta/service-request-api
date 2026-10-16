package com.jnta.solicitation.domain.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jnta.solicitation.domain.exception.InvalidSolicitationDataException;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    @DisplayName("given a major-unit BRL amount, when converted, then it is stored as minor units")
    void shouldConvertMajorUnitsToMinorUnits() {
        var money = Money.fromMajorUnits(new BigDecimal("350.00"), Currency.BRL);

        assertThat(money.amountInMinorUnits()).isEqualTo(35_000);
        assertThat(money.currency()).isEqualTo(Currency.BRL);
    }

    @Test
    @DisplayName("given an amount with excess fractional precision, when converted, then it is rejected without rounding")
    void shouldRejectAmountsThatRequireRounding() {
        assertThatThrownBy(() -> Money.fromMajorUnits(new BigDecimal("10.001"), Currency.BRL))
                .isInstanceOf(InvalidSolicitationDataException.class);
    }

    @Test
    @DisplayName("given an amount outside the supported range, when converted, then it is rejected")
    void shouldRejectOverflowingAmount() {
        assertThatThrownBy(() -> Money.fromMajorUnits(new BigDecimal("92233720368547758.08"), Currency.BRL))
                .isInstanceOf(InvalidSolicitationDataException.class);
    }

    @Test
    @DisplayName("given a negative amount in minor units, when created, then it is rejected")
    void shouldRejectNegativeMinorUnits() {
        assertThatThrownBy(() -> new Money(-1, Currency.BRL))
                .isInstanceOf(InvalidSolicitationDataException.class);
    }

    @Test
    @DisplayName("given a money value, when converted back to major units, then its currency precision is preserved")
    void shouldConvertMinorUnitsBackToMajorUnits() {
        assertThat(new Money(35_000, Currency.BRL).toMajorUnits()).isEqualTo(new BigDecimal("350.00"));
    }
}
