package com.jnta.solicitation.application.policy;

import com.jnta.shared.identity.UserId;
import com.jnta.solicitation.application.exception.SolicitationAccessDeniedException;
import com.jnta.solicitation.domain.Solicitation;

public class SolicitationAccessPolicy {

    public void ensureCanRead(UserId requester, Solicitation solicitation) {
        ensureOwner(requester, solicitation);
    }

    public void ensureCanEdit(UserId requester, Solicitation solicitation) {
        ensureOwner(requester, solicitation);
    }

    private void ensureOwner(UserId requester, Solicitation solicitation) {
        if (!solicitation.owner().equals(requester)) {
            throw new SolicitationAccessDeniedException(solicitation.id());
        }
    }
}
