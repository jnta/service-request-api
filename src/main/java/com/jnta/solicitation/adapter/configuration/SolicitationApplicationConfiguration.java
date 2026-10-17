package com.jnta.solicitation.adapter.configuration;

import com.jnta.solicitation.application.CreateSolicitationUseCase;
import com.jnta.solicitation.application.GetSolicitationUseCase;
import com.jnta.solicitation.application.port.out.SolicitationRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SolicitationApplicationConfiguration {

    @Bean
    CreateSolicitationUseCase createSolicitationUseCase(SolicitationRepository repository) {
        return new CreateSolicitationUseCase(repository);
    }

    @Bean
    GetSolicitationUseCase getSolicitationUseCase(SolicitationRepository repository) {
        return new GetSolicitationUseCase(repository);
    }
}
