package com.jnta.solicitation.adapter.configuration;

import com.jnta.solicitation.application.CreateSolicitationUseCase;
import com.jnta.solicitation.application.GetSolicitationUseCase;
import com.jnta.solicitation.application.UpdateAddressUseCase;
import com.jnta.solicitation.application.UpdateBasicDataUseCase;
import com.jnta.solicitation.application.port.out.PostalCodeGateway;
import com.jnta.solicitation.application.port.out.SolicitationRepository;
import com.jnta.solicitation.application.policy.SolicitationAccessPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SolicitationApplicationConfiguration {

    @Bean
    CreateSolicitationUseCase createSolicitationUseCase(SolicitationRepository repository) {
        return new CreateSolicitationUseCase(repository);
    }

    @Bean
    SolicitationAccessPolicy solicitationAccessPolicy() {
        return new SolicitationAccessPolicy();
    }

    @Bean
    GetSolicitationUseCase getSolicitationUseCase(
            SolicitationRepository repository,
            SolicitationAccessPolicy accessPolicy) {
        return new GetSolicitationUseCase(repository, accessPolicy);
    }

    @Bean
    UpdateBasicDataUseCase updateBasicDataUseCase(
            SolicitationRepository repository,
            SolicitationAccessPolicy accessPolicy) {
        return new UpdateBasicDataUseCase(repository, accessPolicy);
    }

    @Bean
    UpdateAddressUseCase updateAddressUseCase(
            SolicitationRepository repository,
            SolicitationAccessPolicy accessPolicy,
            PostalCodeGateway postalCodeGateway) {
        return new UpdateAddressUseCase(repository, accessPolicy, postalCodeGateway);
    }
}
