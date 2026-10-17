package com.jnta.auth.application.command;

public record RegisterClientCommand(String name, String email, String password) {
}
