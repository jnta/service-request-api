package com.jnta.auth.adapter.out.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.bootstrap-admin")
public record AdminBootstrapProperties(boolean enabled, String name, String email, String password) {

    public AdminBootstrapProperties {
        if (enabled && (name == null || name.isBlank() || email == null || email.isBlank()
                || password == null || password.length() < 12)) {
            throw new IllegalArgumentException("Enabled admin bootstrap requires a name, email, and password of at least 12 characters");
        }
    }
}
