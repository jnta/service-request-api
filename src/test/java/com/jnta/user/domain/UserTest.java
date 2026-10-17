package com.jnta.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jnta.shared.geography.BrazilianState;
import com.jnta.shared.identity.UserId;
import com.jnta.user.domain.exception.InvalidUserDataException;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    @DisplayName("given a valid client, when created, then its email is normalized and it has no coverage")
    void shouldCreateClient() {
        var user = User.registerClient(
                UserId.generate(), " Client ", new EmailAddress(" Client@Example.com "), new PasswordHash("encoded"));

        assertThat(user.name()).isEqualTo("Client");
        assertThat(user.email().value()).isEqualTo("client@example.com");
        assertThat(user.role()).isEqualTo(Role.CLIENT);
        assertThat(user.coverage()).isEmpty();
    }

    @Test
    @DisplayName("given an analyst without coverage, when created, then the user is rejected")
    void shouldRequireCoverageForAnalyst() {
        assertThatThrownBy(() -> User.createManaged(
                        UserId.generate(), "Analyst", email(), new PasswordHash("encoded"), Role.ANALYST, Set.of()))
                .isInstanceOf(InvalidUserDataException.class);
    }

    @Test
    @DisplayName("given a client role, when a managed user is created, then the operation is rejected")
    void shouldNotCreateClientThroughManagedUserFactory() {
        assertThatThrownBy(() -> User.createManaged(
                        UserId.generate(), "Client", email(), new PasswordHash("encoded"), Role.CLIENT, Set.of()))
                .isInstanceOf(InvalidUserDataException.class);
    }

    @Test
    @DisplayName("given an analyst with state coverage, when created, then the coverage is immutable")
    void shouldCopyAnalystCoverage() {
        var coverage = new java.util.HashSet<>(Set.of(BrazilianState.DF, BrazilianState.GO));
        var analyst = User.createManaged(
                UserId.generate(), "Analyst", email(), new PasswordHash("encoded"), Role.ANALYST, coverage);
        coverage.clear();

        assertThat(analyst.coverage()).containsExactlyInAnyOrder(BrazilianState.DF, BrazilianState.GO);
        assertThatThrownBy(() -> analyst.coverage().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("given an invalid email, when created, then it is rejected")
    void shouldRejectInvalidEmail() {
        assertThatThrownBy(() -> new EmailAddress("not-an-email"))
                .isInstanceOf(InvalidUserDataException.class);
    }

    private static EmailAddress email() {
        return new EmailAddress(UUID.randomUUID() + "@example.com");
    }
}
