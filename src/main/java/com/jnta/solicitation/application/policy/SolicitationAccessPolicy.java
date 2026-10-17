package com.jnta.solicitation.application.policy;

import com.jnta.shared.geography.BrazilianState;
import com.jnta.shared.identity.UserId;
import com.jnta.solicitation.domain.Solicitation;
import com.jnta.user.domain.Role;
import com.jnta.user.domain.User;

public class SolicitationAccessPolicy {

    public boolean canRead(User actor, Solicitation solicitation) {
        return switch (actor.role()) {
            case CLIENT -> owns(actor.id(), solicitation);
            case ANALYST -> covers(actor, solicitation);
            case ADMIN -> true;
        };
    }

    public boolean canEdit(User actor, Solicitation solicitation) {
        return switch (actor.role()) {
            case CLIENT -> owns(actor.id(), solicitation);
            case ANALYST -> false;
            case ADMIN -> true;
        };
    }

    public boolean canReview(User actor, Solicitation solicitation) {
        return actor.role() == Role.ADMIN || covers(actor, solicitation);
    }

    public boolean canDecide(User actor, Solicitation solicitation) {
        return canReview(actor, solicitation);
    }

    private boolean covers(User actor, Solicitation solicitation) {
        BrazilianState state = solicitation.address() == null ? null : solicitation.address().state();
        return state != null && actor.coverage().contains(state);
    }

    private boolean owns(UserId actorId, Solicitation solicitation) {
        return solicitation.owner().equals(actorId);
    }
}
