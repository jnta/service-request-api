package com.jnta.solicitation.application;

import com.jnta.shared.identity.UserId;
import com.jnta.solicitation.application.exception.SolicitationNotFoundException;
import com.jnta.solicitation.application.port.out.SolicitationRepository;
import com.jnta.solicitation.application.policy.SolicitationAccessPolicy;
import com.jnta.solicitation.domain.Solicitation;
import com.jnta.solicitation.domain.SolicitationId;

public class GetSolicitationUseCase {

    private final SolicitationRepository solicitationRepository;
    private final SolicitationAccessPolicy accessPolicy;

    public GetSolicitationUseCase(
            SolicitationRepository solicitationRepository,
            SolicitationAccessPolicy accessPolicy) {
        this.solicitationRepository = solicitationRepository;
        this.accessPolicy = accessPolicy;
    }

    public Solicitation get(SolicitationId id, UserId requester) {
        var solicitation = solicitationRepository.findById(id)
                .orElseThrow(() -> new SolicitationNotFoundException(id))
                .solicitation();
        accessPolicy.ensureCanRead(requester, solicitation);
        return solicitation;
    }
}
