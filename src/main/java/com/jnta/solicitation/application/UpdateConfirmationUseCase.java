package com.jnta.solicitation.application;

import com.jnta.shared.identity.UserId;
import com.jnta.solicitation.application.command.UpdateConfirmationCommand;
import com.jnta.solicitation.application.exception.SolicitationNotFoundException;
import com.jnta.solicitation.application.port.out.SolicitationRepository;
import com.jnta.solicitation.application.port.out.VersionedSolicitation;
import com.jnta.solicitation.application.policy.SolicitationAccessPolicy;
import com.jnta.solicitation.domain.Solicitation;
import com.jnta.solicitation.domain.SolicitationId;
import com.jnta.solicitation.domain.confirmation.Confirmation;
import com.jnta.solicitation.domain.money.Money;
import java.time.Clock;

public class UpdateConfirmationUseCase {

    private final SolicitationRepository solicitationRepository;
    private final SolicitationAccessPolicy accessPolicy;
    private final Clock clock;

    public UpdateConfirmationUseCase(
            SolicitationRepository solicitationRepository,
            SolicitationAccessPolicy accessPolicy,
            Clock clock) {
        this.solicitationRepository = solicitationRepository;
        this.accessPolicy = accessPolicy;
        this.clock = clock;
    }

    public Solicitation update(SolicitationId id, UserId requester, UpdateConfirmationCommand command) {
        VersionedSolicitation stored = solicitationRepository.findById(id)
                .orElseThrow(() -> new SolicitationNotFoundException(id));
        Solicitation solicitation = stored.solicitation();
        accessPolicy.ensureCanEdit(requester, solicitation);

        var money = Money.fromMajorUnits(command.estimatedAmount(), command.currency());
        var confirmation = new Confirmation(
                command.priority(), command.preferredDate(), money, Boolean.TRUE.equals(command.termsAccepted()));
        solicitation.updateConfirmation(confirmation, clock);
        return solicitationRepository.save(stored.withSolicitation(solicitation)).solicitation();
    }
}
