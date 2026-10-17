package com.jnta.solicitation.adapter.observability;

import com.jnta.solicitation.domain.review.Decision;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class SolicitationMetrics {

    private final Counter created;
    private final Counter submitted;
    private final Counter approved;
    private final Counter rejected;

    public SolicitationMetrics(MeterRegistry meterRegistry) {
        created = meterRegistry.counter("solicitation.created");
        submitted = meterRegistry.counter("solicitation.submitted");
        approved = meterRegistry.counter("solicitation.approved");
        rejected = meterRegistry.counter("solicitation.rejected");
    }

    public void created() {
        created.increment();
    }

    public void submitted() {
        submitted.increment();
    }

    public void decided(Decision.Outcome outcome) {
        switch (outcome) {
            case APPROVE -> approved.increment();
            case REJECT -> rejected.increment();
        }
    }
}
