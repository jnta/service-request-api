package com.jnta.solicitation.application.exception;

import com.jnta.solicitation.domain.address.Cep;

public class PostalCodeNotFoundException extends RuntimeException {

    public PostalCodeNotFoundException(Cep cep) {
        super("Postal code was not found: " + cep.value());
    }
}
