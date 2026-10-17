package com.jnta.solicitation.adapter.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnta.solicitation.domain.review.Decision;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class SolicitationMetricsTest {

    @Test
    void shouldCountSuccessfulSolicitationLifecycleOperations() {
        var registry = new SimpleMeterRegistry();
        var metrics = new SolicitationMetrics(registry);

        metrics.created();
        metrics.submitted();
        metrics.decided(Decision.Outcome.APPROVE);
        metrics.decided(Decision.Outcome.REJECT);

        assertThat(registry.get("solicitation.created").counter().count()).isEqualTo(1);
        assertThat(registry.get("solicitation.submitted").counter().count()).isEqualTo(1);
        assertThat(registry.get("solicitation.approved").counter().count()).isEqualTo(1);
        assertThat(registry.get("solicitation.rejected").counter().count()).isEqualTo(1);
    }
}
