package com.jnta.user.domain;

import com.jnta.shared.geography.BrazilianState;
import com.jnta.shared.identity.UserId;
import com.jnta.user.domain.exception.InvalidUserDataException;
import java.util.Set;

public record User(
        UserId id,
        String name,
        EmailAddress email,
        PasswordHash passwordHash,
        Role role,
        Set<BrazilianState> coverage) {

    public User {
        if (id == null || email == null || passwordHash == null || role == null || coverage == null) {
            throw new InvalidUserDataException("User identity, credentials, role, and coverage are required");
        }
        if (name == null || name.isBlank() || name.trim().length() > 120) {
            throw new InvalidUserDataException("Name must contain between 1 and 120 characters");
        }
        name = name.trim();
        coverage = Set.copyOf(coverage);
        validateCoverage(role, coverage);
    }

    public static User registerClient(UserId id, String name, EmailAddress email, PasswordHash passwordHash) {
        return new User(id, name, email, passwordHash, Role.CLIENT, Set.of());
    }

    public static User createManaged(
            UserId id,
            String name,
            EmailAddress email,
            PasswordHash passwordHash,
            Role role,
            Set<BrazilianState> coverage) {
        if (role == Role.CLIENT) {
            throw new InvalidUserDataException("Managed user role must be ANALYST or ADMIN");
        }
        return new User(id, name, email, passwordHash, role, coverage);
    }

    private static void validateCoverage(Role role, Set<BrazilianState> coverage) {
        if (role == Role.ANALYST && coverage.isEmpty()) {
            throw new InvalidUserDataException("Analysts must have at least one covered state");
        }
        if (role != Role.ANALYST && !coverage.isEmpty()) {
            throw new InvalidUserDataException("Only analysts can have state coverage");
        }
    }
}
