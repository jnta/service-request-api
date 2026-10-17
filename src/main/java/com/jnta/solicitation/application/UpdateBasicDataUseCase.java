package com.jnta.solicitation.application;

import com.jnta.shared.identity.UserId;
import com.jnta.solicitation.application.command.UpdateBasicDataCommand;
import com.jnta.solicitation.application.exception.SolicitationNotFoundException;
import com.jnta.solicitation.application.port.out.SolicitationRepository;
import com.jnta.solicitation.application.policy.SolicitationAccessPolicy;
import com.jnta.solicitation.domain.Solicitation;
import com.jnta.solicitation.domain.SolicitationId;
import com.jnta.solicitation.domain.basicdata.BasicData;
import com.jnta.solicitation.domain.basicdata.ServiceType;

public class UpdateBasicDataUseCase {

    private final SolicitationRepository solicitationRepository;
    private final SolicitationAccessPolicy accessPolicy;

    public UpdateBasicDataUseCase(
            SolicitationRepository solicitationRepository,
            SolicitationAccessPolicy accessPolicy) {
        this.solicitationRepository = solicitationRepository;
        this.accessPolicy = accessPolicy;
    }

    public Solicitation update(SolicitationId id, UserId requester, UpdateBasicDataCommand command) {
        var stored = solicitationRepository.findById(id)
                .orElseThrow(() -> new SolicitationNotFoundException(id));
        accessPolicy.ensureCanEdit(requester, stored.solicitation());

        var basicData = new BasicData(
                new ServiceType(command.serviceType()), command.title(), command.description());
        stored.solicitation().updateBasicData(basicData);
        return solicitationRepository.save(stored.withSolicitation(stored.solicitation())).solicitation();
    }
}
