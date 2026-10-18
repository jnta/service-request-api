package com.jnta.solicitation.adapter.in.web;

import com.jnta.solicitation.domain.Solicitation;
import com.jnta.solicitation.domain.address.Address;
import com.jnta.solicitation.domain.basicdata.BasicData;
import com.jnta.solicitation.domain.confirmation.Confirmation;
import com.jnta.solicitation.domain.review.Decision;
import org.springframework.stereotype.Component;

@Component
public class SolicitationResponseMapper {

    public SolicitationResponse toResponse(Solicitation solicitation) {
        return new SolicitationResponse(
                solicitation.id().value(),
                solicitation.owner().value(),
                solicitation.status(),
                solicitation.currentStep(),
                map(solicitation.basicData()),
                map(solicitation.address()),
                map(solicitation.confirmation()),
                map(solicitation.decision()));
    }

    private SolicitationResponse.BasicData map(BasicData basicData) {
        if (basicData == null) {
            return null;
        }
        return new SolicitationResponse.BasicData(
                basicData.serviceType().name(), basicData.title(), basicData.description());
    }

    private SolicitationResponse.Address map(Address address) {
        if (address == null) {
            return null;
        }
        return new SolicitationResponse.Address(
                address.cep().value(),
                address.street(),
                address.neighborhood(),
                address.city(),
                address.state(),
                address.number(),
                address.complement());
    }

    private SolicitationResponse.Confirmation map(Confirmation confirmation) {
        if (confirmation == null) {
            return null;
        }
        var money = confirmation.estimatedValue();
        return new SolicitationResponse.Confirmation(
                confirmation.priority(),
                confirmation.preferredDate(),
                new SolicitationResponse.Money(money.toMajorUnits(), money.currency()),
                confirmation.termsAccepted());
    }

    private SolicitationResponse.DecisionDetails map(Decision decision) {
        if (decision == null) {
            return null;
        }
        return new SolicitationResponse.DecisionDetails(decision.outcome(), decision.comment());
    }
}
