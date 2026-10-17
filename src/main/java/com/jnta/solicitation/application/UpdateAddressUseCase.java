package com.jnta.solicitation.application;

import com.jnta.shared.identity.UserId;
import com.jnta.solicitation.application.command.UpdateAddressCommand;
import com.jnta.solicitation.application.exception.SolicitationNotFoundException;
import com.jnta.solicitation.application.port.out.PostalCodeGateway;
import com.jnta.solicitation.application.port.out.SolicitationRepository;
import com.jnta.solicitation.application.port.out.VersionedSolicitation;
import com.jnta.solicitation.application.policy.SolicitationAccessPolicy;
import com.jnta.solicitation.domain.Solicitation;
import com.jnta.solicitation.domain.SolicitationId;
import com.jnta.solicitation.domain.address.Address;
import com.jnta.solicitation.domain.address.Cep;

public class UpdateAddressUseCase {

    private final SolicitationRepository solicitationRepository;
    private final SolicitationAccessPolicy accessPolicy;
    private final PostalCodeGateway postalCodeGateway;

    public UpdateAddressUseCase(
            SolicitationRepository solicitationRepository,
            SolicitationAccessPolicy accessPolicy,
            PostalCodeGateway postalCodeGateway) {
        this.solicitationRepository = solicitationRepository;
        this.accessPolicy = accessPolicy;
        this.postalCodeGateway = postalCodeGateway;
    }

    public Solicitation update(SolicitationId id, UserId requester, UpdateAddressCommand command) {
        VersionedSolicitation stored = solicitationRepository.findById(id)
                .orElseThrow(() -> new SolicitationNotFoundException(id));
        Solicitation solicitation = stored.solicitation();
        accessPolicy.ensureCanEdit(requester, solicitation);
        solicitation.ensureAddressCanBeUpdated();

        var cep = new Cep(command.cep());
        var postalAddress = postalCodeGateway.find(cep);
        var address = new Address(
                cep,
                postalAddress.street(),
                postalAddress.neighborhood(),
                postalAddress.city(),
                postalAddress.state(),
                command.number(),
                command.complement());

        solicitation.updateAddress(address);
        return solicitationRepository.save(stored.withSolicitation(solicitation)).solicitation();
    }
}
