package com.jnta.user.domain;

import com.jnta.user.domain.exception.InvalidUserDataException;

public record PasswordHash(String value) {

    public PasswordHash {
        if (value == null || value.isBlank() || value.length() > 100) {
            throw new InvalidUserDataException("Password hash is invalid");
        }
    }
}
