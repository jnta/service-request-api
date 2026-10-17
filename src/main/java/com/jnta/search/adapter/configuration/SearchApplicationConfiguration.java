package com.jnta.search.adapter.configuration;

import com.jnta.search.application.SearchSolicitationsUseCase;
import com.jnta.search.application.port.out.SolicitationSearchRepository;
import com.jnta.user.application.port.out.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SearchApplicationConfiguration {

    @Bean
    SearchSolicitationsUseCase searchSolicitationsUseCase(
            UserRepository userRepository,
            SolicitationSearchRepository searchRepository) {
        return new SearchSolicitationsUseCase(userRepository, searchRepository);
    }

}
