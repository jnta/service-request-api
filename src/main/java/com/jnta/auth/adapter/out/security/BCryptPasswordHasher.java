package com.jnta.auth.adapter.out.security;

import com.jnta.user.application.port.out.PasswordHasher;
import com.jnta.user.domain.PasswordHash;
import com.jnta.user.domain.exception.InvalidUserDataException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BCryptPasswordHasher implements PasswordHasher {

    private static final int BCRYPT_MAXIMUM_PASSWORD_BYTES = 72;

    private final PasswordEncoder passwordEncoder;

    @Override
    public PasswordHash hash(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new InvalidUserDataException("Password is required");
        }
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAXIMUM_PASSWORD_BYTES) {
            throw new InvalidUserDataException("Password exceeds the supported length");
        }
        return new PasswordHash(passwordEncoder.encode(rawPassword));
    }

    @Override
    public boolean matches(String rawPassword, PasswordHash passwordHash) {
        if (rawPassword == null || passwordHash == null
                || rawPassword.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAXIMUM_PASSWORD_BYTES) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, passwordHash.value());
    }
}
