package com.jnta.solicitation.application;

import com.jnta.shared.identity.UserId;
import com.jnta.solicitation.application.exception.SolicitationAccessDeniedException;
import com.jnta.solicitation.application.exception.SolicitationNotFoundException;
import com.jnta.solicitation.application.port.out.SolicitationRepository;
import com.jnta.solicitation.domain.Solicitation;
import com.jnta.solicitation.domain.SolicitationId;

public class GetSolicitationUseCase {

    private final SolicitationRepository solicitationRepository;

    public GetSolicitationUseCase(SolicitationRepository solicitationRepository) {
        this.solicitationRepository = solicitationRepository;
    }

    public Solicitation get(SolicitationId id, UserId requester) {
        var solicitation = solicitationRepository.findById(id)
                .orElseThrow(() -> new SolicitationNotFoundException(id))
                .solicitation();
        if (!solicitation.owner().equals(requester)) {
            throw new SolicitationAccessDeniedException(id);
        }
        return solicitation;
    }
}
