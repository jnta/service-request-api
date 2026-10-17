package com.jnta.auth.adapter.out.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(String secretBase64, String issuer, Duration accessTokenTtl) {

    public JwtProperties {
        if (secretBase64 == null || secretBase64.isBlank()) {
            throw new IllegalArgumentException("JWT secret is required");
        }
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("JWT issuer is required");
        }
        if (accessTokenTtl == null || accessTokenTtl.isNegative() || accessTokenTtl.isZero()) {
            throw new IllegalArgumentException("JWT access token lifetime must be positive");
        }
    }
}
