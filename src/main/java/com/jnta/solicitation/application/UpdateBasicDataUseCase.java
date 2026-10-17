package com.jnta.solicitation.application;

import com.jnta.audit.application.annotation.Audit;
import com.jnta.audit.domain.AuditAction;
import com.jnta.shared.identity.UserId;
import com.jnta.solicitation.application.command.UpdateBasicDataCommand;
import com.jnta.solicitation.application.exception.SolicitationNotFoundException;
import com.jnta.solicitation.application.exception.SolicitationAccessDeniedException;
import com.jnta.solicitation.application.port.out.SolicitationRepository;
import com.jnta.solicitation.application.policy.SolicitationAccessPolicy;
import com.jnta.solicitation.domain.Solicitation;
import com.jnta.solicitation.domain.SolicitationId;
import com.jnta.solicitation.domain.basicdata.BasicData;
import com.jnta.solicitation.domain.basicdata.ServiceType;
import com.jnta.user.application.port.out.UserRepository;

public class UpdateBasicDataUseCase {

    private final SolicitationRepository solicitationRepository;
    private final UserRepository userRepository;
    private final SolicitationAccessPolicy accessPolicy;

    public UpdateBasicDataUseCase(
            SolicitationRepository solicitationRepository,
            UserRepository userRepository,
            SolicitationAccessPolicy accessPolicy) {
        this.solicitationRepository = solicitationRepository;
        this.userRepository = userRepository;
        this.accessPolicy = accessPolicy;
    }

    @Audit(action = AuditAction.SOLICITATION_UPDATED)
    public Solicitation update(SolicitationId id, UserId requester, UpdateBasicDataCommand command) {
        var stored = solicitationRepository.findById(id)
                .orElseThrow(() -> new SolicitationNotFoundException(id));
        var actor = userRepository.findById(requester)
                .orElseThrow(() -> new SolicitationAccessDeniedException(id));
        if (!accessPolicy.canEdit(actor, stored.solicitation())) {
            throw new SolicitationAccessDeniedException(id);
        }

        var basicData = new BasicData(
                new ServiceType(command.serviceType()), command.title(), command.description());
        stored.solicitation().updateBasicData(basicData);
        return solicitationRepository.save(stored.withSolicitation(stored.solicitation())).solicitation();
    }
}
