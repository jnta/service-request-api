package com.jnta.solicitation.domain;

import com.jnta.solicitation.domain.exception.InvalidSolicitationDataException;
import java.util.Locale;

public record ServiceType(String value) {

    public ServiceType {
        if (value == null || !value.trim().matches("[A-Za-z][A-Za-z0-9_]{0,39}")) {
            throw new InvalidSolicitationDataException("Service type must be a valid code");
        }
        value = value.trim().toUpperCase(Locale.ROOT);
    }
}
