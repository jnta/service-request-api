package com.jnta.solicitation.application;

import com.jnta.shared.identity.UserId;
import com.jnta.solicitation.application.exception.SolicitationNotFoundException;
import com.jnta.solicitation.application.port.out.SolicitationRepository;
import com.jnta.solicitation.application.port.out.VersionedSolicitation;
import com.jnta.solicitation.application.policy.SolicitationAccessPolicy;
import com.jnta.solicitation.domain.Solicitation;
import com.jnta.solicitation.domain.SolicitationId;
import java.time.Clock;

public class SubmitSolicitationUseCase {

    private final SolicitationRepository solicitationRepository;
    private final SolicitationAccessPolicy accessPolicy;
    private final Clock clock;

    public SubmitSolicitationUseCase(
            SolicitationRepository solicitationRepository,
            SolicitationAccessPolicy accessPolicy,
            Clock clock) {
        this.solicitationRepository = solicitationRepository;
        this.accessPolicy = accessPolicy;
        this.clock = clock;
    }

    public Solicitation submit(SolicitationId id, UserId requester) {
        VersionedSolicitation stored = solicitationRepository.findById(id)
                .orElseThrow(() -> new SolicitationNotFoundException(id));
        Solicitation solicitation = stored.solicitation();
        accessPolicy.ensureCanEdit(requester, solicitation);
        solicitation.submit(clock);
        return solicitationRepository.save(stored.withSolicitation(solicitation)).solicitation();
    }
}
