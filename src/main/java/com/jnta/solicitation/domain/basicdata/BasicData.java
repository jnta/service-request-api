package com.jnta.solicitation.domain.basicdata;

import com.jnta.solicitation.domain.exception.InvalidSolicitationDataException;

public record BasicData(ServiceType serviceType, String title, String description) {

    public BasicData {
        if (serviceType == null) {
            throw new InvalidSolicitationDataException("Service type is required");
        }
        title = required(title, "Title");
        description = required(description, "Description");
        validateLength(title, 3, 80, "Title");
        validateLength(description, 20, 1000, "Description");
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidSolicitationDataException(field + " is required");
        }
        return value.trim();
    }

    private static void validateLength(String value, int minimum, int maximum, String field) {
        if (value.length() < minimum || value.length() > maximum) {
            throw new InvalidSolicitationDataException(
                    field + " must contain between " + minimum + " and " + maximum + " characters");
        }
    }
}
