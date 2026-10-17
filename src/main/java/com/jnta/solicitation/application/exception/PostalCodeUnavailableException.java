package com.jnta.solicitation.application.exception;

public class PostalCodeUnavailableException extends RuntimeException {

    public PostalCodeUnavailableException(String message) {
        super(message);
    }

    public PostalCodeUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
