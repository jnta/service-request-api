package com.jnta.user.domain;

import com.jnta.user.domain.exception.InvalidUserDataException;
import java.util.Locale;

public record EmailAddress(String value) {

    public EmailAddress {
        if (value == null) {
            throw new InvalidUserDataException("Email is required");
        }
        value = value.trim().toLowerCase(Locale.ROOT);
        if (value.length() > 320 || !value.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new InvalidUserDataException("Email is invalid");
        }
    }
}
