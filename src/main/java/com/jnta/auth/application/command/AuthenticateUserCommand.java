package com.jnta.auth.application.command;

public record AuthenticateUserCommand(String email, String password) {
}
