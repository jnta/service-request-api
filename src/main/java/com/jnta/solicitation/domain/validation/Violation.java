package com.jnta.solicitation.domain.validation;

public record Violation(String field, String message) {

    public Violation {
        if (field == null || field.isBlank() || message == null || message.isBlank()) {
            throw new IllegalArgumentException("Violation field and message are required");
        }
    }
}
