package com.jnta.solicitation.domain;

import com.jnta.solicitation.domain.exception.InvalidSolicitationDataException;

public record Address(
        Cep cep,
        String street,
        String neighborhood,
        String city,
        State state,
        String number,
        String complement) {

    public Address {
        if (cep == null) {
            throw new InvalidSolicitationDataException("CEP is required");
        }
        street = required(street, "Street");
        neighborhood = required(neighborhood, "Neighborhood");
        city = required(city, "City");
        if (state == null) {
            throw new InvalidSolicitationDataException("State is required");
        }
        number = required(number, "Address number");
        if (number.length() > 20) {
            throw new InvalidSolicitationDataException("Address number must contain at most 20 characters");
        }
        complement = complement == null || complement.isBlank() ? null : complement.trim();
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidSolicitationDataException(field + " is required");
        }
        return value.trim();
    }
}
