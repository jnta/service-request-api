package com.jnta.solicitation.application;

import com.jnta.audit.application.annotation.Audit;
import com.jnta.audit.domain.AuditAction;
import com.jnta.shared.identity.UserId;
import com.jnta.solicitation.application.command.DecideSolicitationCommand;
import com.jnta.solicitation.application.exception.SolicitationAccessDeniedException;
import com.jnta.solicitation.application.exception.SolicitationNotFoundException;
import com.jnta.solicitation.application.port.out.SolicitationRepository;
import com.jnta.solicitation.application.port.out.VersionedSolicitation;
import com.jnta.solicitation.application.policy.SolicitationAccessPolicy;
import com.jnta.solicitation.domain.Solicitation;
import com.jnta.solicitation.domain.SolicitationId;
import com.jnta.solicitation.domain.exception.InvalidSolicitationDataException;
import com.jnta.user.application.port.out.UserRepository;

public class DecideSolicitationUseCase {

    private final SolicitationRepository solicitationRepository;
    private final UserRepository userRepository;
    private final SolicitationAccessPolicy accessPolicy;

    public DecideSolicitationUseCase(
            SolicitationRepository solicitationRepository,
            UserRepository userRepository,
            SolicitationAccessPolicy accessPolicy) {
        this.solicitationRepository = solicitationRepository;
        this.userRepository = userRepository;
        this.accessPolicy = accessPolicy;
    }

    @Audit(action = AuditAction.SOLICITATION_DECIDED)
    public Solicitation decide(SolicitationId id, UserId reviewerId, DecideSolicitationCommand command) {
        VersionedSolicitation stored = solicitationRepository.findById(id)
                .orElseThrow(() -> new SolicitationNotFoundException(id));
        Solicitation solicitation = stored.solicitation();
        var reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new SolicitationAccessDeniedException(id));
        if (!accessPolicy.canDecide(reviewer, solicitation)) {
            throw new SolicitationAccessDeniedException(id);
        }
        if (command.decision() == null) {
            throw new InvalidSolicitationDataException("Decision is required");
        }
        switch (command.decision()) {
            case APPROVE -> solicitation.approve(command.comment());
            case REJECT -> solicitation.reject(command.comment());
        }
        return solicitationRepository.save(stored.withSolicitation(solicitation)).solicitation();
    }
}
