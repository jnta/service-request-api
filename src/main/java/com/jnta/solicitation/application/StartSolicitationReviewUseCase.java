package com.jnta.solicitation.application;

import com.jnta.audit.application.annotation.Audit;
import com.jnta.audit.domain.AuditAction;
import com.jnta.shared.identity.UserId;
import com.jnta.solicitation.application.exception.SolicitationAccessDeniedException;
import com.jnta.solicitation.application.exception.SolicitationNotFoundException;
import com.jnta.solicitation.application.port.out.SolicitationRepository;
import com.jnta.solicitation.application.port.out.VersionedSolicitation;
import com.jnta.solicitation.application.policy.SolicitationAccessPolicy;
import com.jnta.solicitation.domain.Solicitation;
import com.jnta.solicitation.domain.SolicitationId;
import com.jnta.user.application.port.out.UserRepository;

public class StartSolicitationReviewUseCase {

    private final SolicitationRepository solicitationRepository;
    private final UserRepository userRepository;
    private final SolicitationAccessPolicy accessPolicy;

    public StartSolicitationReviewUseCase(
            SolicitationRepository solicitationRepository,
            UserRepository userRepository,
            SolicitationAccessPolicy accessPolicy) {
        this.solicitationRepository = solicitationRepository;
        this.userRepository = userRepository;
        this.accessPolicy = accessPolicy;
    }

    @Audit(action = AuditAction.SOLICITATION_REVIEW_STARTED)
    public Solicitation start(SolicitationId id, UserId reviewerId) {
        VersionedSolicitation stored = solicitationRepository.findById(id)
                .orElseThrow(() -> new SolicitationNotFoundException(id));
        Solicitation solicitation = stored.solicitation();
        var reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new SolicitationAccessDeniedException(id));
        if (!accessPolicy.canReview(reviewer, solicitation)) {
            throw new SolicitationAccessDeniedException(id);
        }
        solicitation.startReview();
        return solicitationRepository.save(stored.withSolicitation(solicitation)).solicitation();
    }
}
