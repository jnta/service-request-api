package com.jnta.solicitation.adapter.configuration;

import com.jnta.solicitation.application.CreateSolicitationUseCase;
import com.jnta.solicitation.application.DecideSolicitationUseCase;
import com.jnta.solicitation.application.GetSolicitationUseCase;
import com.jnta.solicitation.application.StartSolicitationReviewUseCase;
import com.jnta.solicitation.application.SubmitSolicitationUseCase;
import com.jnta.solicitation.application.UpdateAddressUseCase;
import com.jnta.solicitation.application.UpdateBasicDataUseCase;
import com.jnta.solicitation.application.UpdateConfirmationUseCase;
import com.jnta.solicitation.application.port.out.PostalCodeGateway;
import com.jnta.solicitation.application.port.out.SolicitationRepository;
import com.jnta.solicitation.application.policy.SolicitationAccessPolicy;
import com.jnta.user.application.port.out.UserRepository;
import java.time.Clock;
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
            UserRepository users,
            SolicitationAccessPolicy accessPolicy) {
        return new GetSolicitationUseCase(repository, users, accessPolicy);
    }

    @Bean
    UpdateBasicDataUseCase updateBasicDataUseCase(
            SolicitationRepository repository,
            UserRepository users,
            SolicitationAccessPolicy accessPolicy) {
        return new UpdateBasicDataUseCase(repository, users, accessPolicy);
    }

    @Bean
    UpdateAddressUseCase updateAddressUseCase(
            SolicitationRepository repository,
            UserRepository users,
            SolicitationAccessPolicy accessPolicy,
            PostalCodeGateway postalCodeGateway) {
        return new UpdateAddressUseCase(repository, users, accessPolicy, postalCodeGateway);
    }

    @Bean
    UpdateConfirmationUseCase updateConfirmationUseCase(
            SolicitationRepository repository,
            UserRepository users,
            SolicitationAccessPolicy accessPolicy,
            Clock clock) {
        return new UpdateConfirmationUseCase(repository, users, accessPolicy, clock);
    }

    @Bean
    SubmitSolicitationUseCase submitSolicitationUseCase(
            SolicitationRepository repository,
            UserRepository users,
            SolicitationAccessPolicy accessPolicy,
            Clock clock) {
        return new SubmitSolicitationUseCase(repository, users, accessPolicy, clock);
    }

    @Bean
    StartSolicitationReviewUseCase startSolicitationReviewUseCase(
            SolicitationRepository repository,
            UserRepository users,
            SolicitationAccessPolicy accessPolicy) {
        return new StartSolicitationReviewUseCase(repository, users, accessPolicy);
    }

    @Bean
    DecideSolicitationUseCase decideSolicitationUseCase(
            SolicitationRepository repository,
            UserRepository users,
            SolicitationAccessPolicy accessPolicy) {
        return new DecideSolicitationUseCase(repository, users, accessPolicy);
    }
}
