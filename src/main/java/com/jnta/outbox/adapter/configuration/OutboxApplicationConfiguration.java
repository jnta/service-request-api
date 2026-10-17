package com.jnta.outbox.adapter.configuration;

import com.jnta.outbox.application.OutboxProcessor;
import com.jnta.outbox.application.port.out.OutboxEventRepository;
import com.jnta.solicitation.application.port.out.SolicitationRepository;
import com.jnta.solicitation.application.port.out.SolicitationSearchProjection;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OutboxApplicationConfiguration {

    @Bean
    OutboxProcessor outboxProcessor(
            OutboxEventRepository outboxEventRepository,
            SolicitationRepository solicitationRepository,
            SolicitationSearchProjection searchProjection,
            Clock clock) {
        return new OutboxProcessor(outboxEventRepository, solicitationRepository, searchProjection, clock);
    }
}
