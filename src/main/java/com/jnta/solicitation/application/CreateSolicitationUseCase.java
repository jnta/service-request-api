package com.jnta.solicitation.application;

import com.jnta.shared.identity.UserId;
import com.jnta.solicitation.application.port.out.SolicitationRepository;
import com.jnta.solicitation.application.port.out.VersionedSolicitation;
import com.jnta.solicitation.domain.Solicitation;
import com.jnta.solicitation.domain.SolicitationId;

public class CreateSolicitationUseCase {

    private final SolicitationRepository solicitationRepository;

    public CreateSolicitationUseCase(SolicitationRepository solicitationRepository) {
        this.solicitationRepository = solicitationRepository;
    }

    public Solicitation create(UserId owner) {
        var solicitation = Solicitation.create(SolicitationId.generate(), owner);
        return solicitationRepository.save(VersionedSolicitation.newEntry(solicitation)).solicitation();
    }
}
