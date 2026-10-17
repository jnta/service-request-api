package com.jnta.user.application.exception;

import com.jnta.user.domain.EmailAddress;

public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException(EmailAddress email) {
        super("Email is already registered: " + email.value());
    }
}
