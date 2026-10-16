package com.jnta.solicitation.domain.money;

import com.jnta.solicitation.domain.exception.InvalidSolicitationDataException;
import java.math.BigDecimal;
import java.math.RoundingMode;

public record Money(long amountInMinorUnits, Currency currency) {

    public Money {
        if (currency == null) {
            throw new InvalidSolicitationDataException("Currency is required");
        }
        if (amountInMinorUnits < 0) {
            throw new InvalidSolicitationDataException("Amount cannot be negative");
        }
    }

    public static Money fromMajorUnits(BigDecimal amount, Currency currency) {
        if (amount == null || currency == null) {
            throw new InvalidSolicitationDataException("Amount and currency are required");
        }
        try {
            var normalized = amount.setScale(currency.minorUnitDigits(), RoundingMode.UNNECESSARY);
            return new Money(normalized.movePointRight(currency.minorUnitDigits()).longValueExact(), currency);
        } catch (ArithmeticException exception) {
            throw new InvalidSolicitationDataException("Amount has unsupported precision or exceeds the supported range");
        }
    }

    public boolean isLessThan(Money other) {
        if (other == null || currency != other.currency) {
            throw new InvalidSolicitationDataException("Money values must use the same currency");
        }
        return amountInMinorUnits < other.amountInMinorUnits;
    }

    public BigDecimal toMajorUnits() {
        return BigDecimal.valueOf(amountInMinorUnits, currency.minorUnitDigits());
    }
}
