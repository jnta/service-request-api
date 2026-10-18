package com.jnta.solicitation.domain.basicdata;

import com.jnta.solicitation.domain.exception.InvalidSolicitationDataException;
import java.util.Locale;

public enum ServiceType {
    INSTALLATION,
    MAINTENANCE,
    INSPECTION;

    public static ServiceType from(String value) {
        if (value == null) {
            throw invalidServiceType();
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw invalidServiceType();
        }
    }

    private static InvalidSolicitationDataException invalidServiceType() {
        return new InvalidSolicitationDataException(
                "Service type must be INSTALLATION, MAINTENANCE, or INSPECTION");
    }
}
