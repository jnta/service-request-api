package com.jnta.solicitation.domain.money;

public enum Currency {
    BRL(2);

    private final int minorUnitDigits;

    Currency(int minorUnitDigits) {
        this.minorUnitDigits = minorUnitDigits;
    }

    public int minorUnitDigits() {
        return minorUnitDigits;
    }
}
