package com.jnta.solicitation.adapter.out.persistence;

import com.jnta.shared.identity.UserId;
import com.jnta.solicitation.application.port.out.VersionedSolicitation;
import com.jnta.solicitation.domain.Solicitation;
import com.jnta.solicitation.domain.SolicitationId;
import com.jnta.solicitation.domain.address.Address;
import com.jnta.solicitation.domain.address.Cep;
import com.jnta.solicitation.domain.basicdata.BasicData;
import com.jnta.solicitation.domain.basicdata.ServiceType;
import com.jnta.solicitation.domain.confirmation.Confirmation;
import com.jnta.solicitation.domain.money.Money;
import com.jnta.solicitation.domain.review.Decision;
import java.util.UUID;

final class SolicitationJpaMapper {

    SolicitationJpaEntity toEntity(VersionedSolicitation entry) {
        var solicitation = entry.solicitation();
        var entity = new SolicitationJpaEntity();
        entity.id = solicitation.id().value();
        entity.clientId = solicitation.owner().value();
        entity.status = solicitation.status();
        entity.version = entry.version().isPresent() ? entry.version().getAsLong() : null;

        var basicData = solicitation.basicData();
        if (basicData != null) {
            entity.serviceType = basicData.serviceType().value();
            entity.title = basicData.title();
            entity.description = basicData.description();
        }

        var address = solicitation.address();
        if (address != null) {
            entity.cep = address.cep().value();
            entity.street = address.street();
            entity.neighborhood = address.neighborhood();
            entity.city = address.city();
            entity.state = address.state();
            entity.addressNumber = address.number();
            entity.complement = address.complement();
        }

        var confirmation = solicitation.confirmation();
        if (confirmation != null) {
            entity.priority = confirmation.priority();
            entity.preferredDate = confirmation.preferredDate();
            entity.estimatedValueMinorUnits = confirmation.estimatedValue().amountInMinorUnits();
            entity.estimatedValueCurrency = confirmation.estimatedValue().currency();
            entity.termsAccepted = confirmation.termsAccepted();
        }

        var decision = solicitation.decision();
        if (decision != null) {
            entity.decision = decision.outcome();
            entity.decisionComment = decision.comment();
        }
        return entity;
    }

    VersionedSolicitation toVersionedSolicitation(SolicitationJpaEntity entity) {
        var basicData = entity.serviceType == null
                ? null
                : new BasicData(new ServiceType(entity.serviceType), entity.title, entity.description);
        var address = entity.cep == null
                ? null
                : new Address(new Cep(entity.cep), entity.street, entity.neighborhood, entity.city, entity.state,
                        entity.addressNumber, entity.complement);
        var confirmation = entity.priority == null
                ? null
                : new Confirmation(entity.priority, entity.preferredDate,
                        new Money(entity.estimatedValueMinorUnits, entity.estimatedValueCurrency), entity.termsAccepted);
        var decision = entity.decision == null ? null : new Decision(entity.decision, entity.decisionComment);
        var solicitation = Solicitation.reconstitute(
                new SolicitationId(entity.id),
                new UserId(entity.clientId),
                entity.status,
                basicData,
                address,
                confirmation,
                decision);
        return VersionedSolicitation.persisted(solicitation, entity.version);
    }
}
