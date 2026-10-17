package com.jnta.solicitation.application;

import com.jnta.audit.application.annotation.Audit;
import com.jnta.audit.domain.AuditAction;
import com.jnta.shared.identity.UserId;
import com.jnta.solicitation.application.exception.SolicitationNotFoundException;
import com.jnta.solicitation.application.exception.SolicitationAccessDeniedException;
import com.jnta.solicitation.application.port.out.SolicitationRepository;
import com.jnta.solicitation.application.port.out.VersionedSolicitation;
import com.jnta.solicitation.application.policy.SolicitationAccessPolicy;
import com.jnta.solicitation.domain.Solicitation;
import com.jnta.solicitation.domain.SolicitationId;
import com.jnta.user.application.port.out.UserRepository;
import java.time.Clock;

public class SubmitSolicitationUseCase {

    private final SolicitationRepository solicitationRepository;
    private final UserRepository userRepository;
    private final SolicitationAccessPolicy accessPolicy;
    private final Clock clock;

    public SubmitSolicitationUseCase(
            SolicitationRepository solicitationRepository,
            UserRepository userRepository,
            SolicitationAccessPolicy accessPolicy,
            Clock clock) {
        this.solicitationRepository = solicitationRepository;
        this.userRepository = userRepository;
        this.accessPolicy = accessPolicy;
        this.clock = clock;
    }

    @Audit(action = AuditAction.SOLICITATION_SUBMITTED)
    public Solicitation submit(SolicitationId id, UserId requester) {
        VersionedSolicitation stored = solicitationRepository.findById(id)
                .orElseThrow(() -> new SolicitationNotFoundException(id));
        Solicitation solicitation = stored.solicitation();
        var actor = userRepository.findById(requester)
                .orElseThrow(() -> new SolicitationAccessDeniedException(id));
        if (!accessPolicy.canEdit(actor, solicitation)) {
            throw new SolicitationAccessDeniedException(id);
        }
        solicitation.submit(clock);
        return solicitationRepository.save(stored.withSolicitation(solicitation)).solicitation();
    }
}
