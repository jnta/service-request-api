package com.jnta.solicitation.domain;

import com.jnta.solicitation.domain.exception.InvalidSolicitationDataException;

public record Cep(String value) {

    public Cep {
        if (value == null) {
            throw new InvalidSolicitationDataException("CEP must contain 8 digits");
        }
        value = value.trim();
        if (!value.matches("\\d{5}-?\\d{3}")) {
            throw new InvalidSolicitationDataException("CEP must contain 8 digits");
        }
        value = value.replace("-", "");
    }
}
