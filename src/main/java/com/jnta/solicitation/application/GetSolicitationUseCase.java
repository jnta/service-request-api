package com.jnta.solicitation.application;

import com.jnta.shared.identity.UserId;
import com.jnta.solicitation.application.exception.SolicitationNotFoundException;
import com.jnta.solicitation.application.exception.SolicitationAccessDeniedException;
import com.jnta.solicitation.application.port.out.SolicitationRepository;
import com.jnta.solicitation.application.policy.SolicitationAccessPolicy;
import com.jnta.solicitation.domain.Solicitation;
import com.jnta.solicitation.domain.SolicitationId;
import com.jnta.user.application.port.out.UserRepository;

public class GetSolicitationUseCase {

    private final SolicitationRepository solicitationRepository;
    private final UserRepository userRepository;
    private final SolicitationAccessPolicy accessPolicy;

    public GetSolicitationUseCase(
            SolicitationRepository solicitationRepository,
            UserRepository userRepository,
            SolicitationAccessPolicy accessPolicy) {
        this.solicitationRepository = solicitationRepository;
        this.userRepository = userRepository;
        this.accessPolicy = accessPolicy;
    }

    public Solicitation get(SolicitationId id, UserId requester) {
        var solicitation = solicitationRepository.findById(id)
                .orElseThrow(() -> new SolicitationNotFoundException(id))
                .solicitation();
        var actor = userRepository.findById(requester)
                .orElseThrow(() -> new SolicitationAccessDeniedException(id));
        if (!accessPolicy.canRead(actor, solicitation)) {
            throw new SolicitationAccessDeniedException(id);
        }
        return solicitation;
    }
}
