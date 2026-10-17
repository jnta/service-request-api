package com.jnta.solicitation.application.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnta.shared.geography.BrazilianState;
import com.jnta.shared.identity.UserId;
import com.jnta.solicitation.domain.Solicitation;
import com.jnta.solicitation.domain.SolicitationId;
import com.jnta.solicitation.domain.address.Address;
import com.jnta.solicitation.domain.address.Cep;
import com.jnta.solicitation.domain.basicdata.BasicData;
import com.jnta.solicitation.domain.basicdata.ServiceType;
import com.jnta.user.domain.EmailAddress;
import com.jnta.user.domain.PasswordHash;
import com.jnta.user.domain.Role;
import com.jnta.user.domain.User;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SolicitationAccessPolicyTest {

    private final SolicitationAccessPolicy policy = new SolicitationAccessPolicy();

    @Test
    @DisplayName("given an analyst covering DF and GO, when the solicitation belongs to DF, then read and review access are allowed")
    void shouldAllowAnalystAccessToCoveredState() {
        var analyst = user(Role.ANALYST, Set.of(BrazilianState.DF, BrazilianState.GO));
        var solicitation = solicitationIn(BrazilianState.DF);

        assertThat(policy.canRead(analyst, solicitation)).isTrue();
        assertThat(policy.canReview(analyst, solicitation)).isTrue();
        assertThat(policy.canDecide(analyst, solicitation)).isTrue();
    }

    @Test
    @DisplayName("given an analyst covering DF, when the solicitation belongs to SP, then access is denied")
    void shouldDenyAnalystAccessToUncoveredState() {
        var analyst = user(Role.ANALYST, Set.of(BrazilianState.DF));
        var solicitation = solicitationIn(BrazilianState.SP);

        assertThat(policy.canRead(analyst, solicitation)).isFalse();
        assertThat(policy.canReview(analyst, solicitation)).isFalse();
        assertThat(policy.canDecide(analyst, solicitation)).isFalse();
    }

    @Test
    @DisplayName("given an ADMIN, when the solicitation belongs to any state, then all access is allowed")
    void shouldAllowAdminAccessToAnyState() {
        var admin = user(Role.ADMIN, Set.of());
        var solicitation = solicitationIn(BrazilianState.AM);

        assertThat(policy.canRead(admin, solicitation)).isTrue();
        assertThat(policy.canEdit(admin, solicitation)).isTrue();
        assertThat(policy.canReview(admin, solicitation)).isTrue();
        assertThat(policy.canDecide(admin, solicitation)).isTrue();
    }

    @Test
    @DisplayName("given a CLIENT, when accessing solicitations, then only the owned solicitation can be read or edited")
    void shouldRestrictClientAccessToOwnedSolicitation() {
        var client = user(Role.CLIENT, Set.of());
        var owned = solicitationOwnedBy(client.id());
        var other = solicitationIn(BrazilianState.DF);

        assertThat(policy.canRead(client, owned)).isTrue();
        assertThat(policy.canEdit(client, owned)).isTrue();
        assertThat(policy.canRead(client, other)).isFalse();
        assertThat(policy.canEdit(client, other)).isFalse();
        assertThat(policy.canReview(client, owned)).isFalse();
        assertThat(policy.canDecide(client, owned)).isFalse();
    }

    @Test
    @DisplayName("given an ANALYST, when editing a covered solicitation is attempted, then edit access is denied")
    void shouldNotAllowAnalystToEditSolicitation() {
        var analyst = user(Role.ANALYST, Set.of(BrazilianState.DF));

        assertThat(policy.canEdit(analyst, solicitationIn(BrazilianState.DF))).isFalse();
    }

    private static User user(Role role, Set<BrazilianState> coverage) {
        return role == Role.CLIENT
                ? User.registerClient(UserId.generate(), "Test User", email(), new PasswordHash("encoded"))
                : User.createManaged(UserId.generate(), "Test User", email(), new PasswordHash("encoded"), role, coverage);
    }

    private static Solicitation solicitationIn(BrazilianState state) {
        var solicitation = Solicitation.create(SolicitationId.generate(), UserId.generate());
        solicitation.updateBasicData(new BasicData(
                new ServiceType("INSTALLATION"), "Equipment installation", "Install equipment at the main office."));
        solicitation.updateAddress(new Address(new Cep("70040902"), "Street", "Neighborhood", "City", state,
                "123", null));
        return solicitation;
    }

    private static Solicitation solicitationOwnedBy(UserId owner) {
        return Solicitation.create(SolicitationId.generate(), owner);
    }

    private static EmailAddress email() {
        return new EmailAddress(UUID.randomUUID() + "@example.com");
    }
}
