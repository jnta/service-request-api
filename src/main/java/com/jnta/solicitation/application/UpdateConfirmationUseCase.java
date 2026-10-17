package com.jnta.solicitation.application;

import com.jnta.audit.application.annotation.Audit;
import com.jnta.audit.domain.AuditAction;
import com.jnta.shared.identity.UserId;
import com.jnta.solicitation.application.command.UpdateConfirmationCommand;
import com.jnta.solicitation.application.exception.SolicitationNotFoundException;
import com.jnta.solicitation.application.exception.SolicitationAccessDeniedException;
import com.jnta.solicitation.application.port.out.SolicitationRepository;
import com.jnta.solicitation.application.port.out.VersionedSolicitation;
import com.jnta.solicitation.application.policy.SolicitationAccessPolicy;
import com.jnta.solicitation.domain.Solicitation;
import com.jnta.solicitation.domain.SolicitationId;
import com.jnta.solicitation.domain.confirmation.Confirmation;
import com.jnta.solicitation.domain.money.Money;
import com.jnta.user.application.port.out.UserRepository;
import java.time.Clock;

public class UpdateConfirmationUseCase {

    private final SolicitationRepository solicitationRepository;
    private final UserRepository userRepository;
    private final SolicitationAccessPolicy accessPolicy;
    private final Clock clock;

    public UpdateConfirmationUseCase(
            SolicitationRepository solicitationRepository,
            UserRepository userRepository,
            SolicitationAccessPolicy accessPolicy,
            Clock clock) {
        this.solicitationRepository = solicitationRepository;
        this.userRepository = userRepository;
        this.accessPolicy = accessPolicy;
        this.clock = clock;
    }

    @Audit(action = AuditAction.SOLICITATION_UPDATED)
    public Solicitation update(SolicitationId id, UserId requester, UpdateConfirmationCommand command) {
        VersionedSolicitation stored = solicitationRepository.findById(id)
                .orElseThrow(() -> new SolicitationNotFoundException(id));
        Solicitation solicitation = stored.solicitation();
        var actor = userRepository.findById(requester)
                .orElseThrow(() -> new SolicitationAccessDeniedException(id));
        if (!accessPolicy.canEdit(actor, solicitation)) {
            throw new SolicitationAccessDeniedException(id);
        }

        var money = Money.fromMajorUnits(command.estimatedAmount(), command.currency());
        var confirmation = new Confirmation(
                command.priority(), command.preferredDate(), money, Boolean.TRUE.equals(command.termsAccepted()));
        solicitation.updateConfirmation(confirmation, clock);
        return solicitationRepository.save(stored.withSolicitation(solicitation)).solicitation();
    }
}
