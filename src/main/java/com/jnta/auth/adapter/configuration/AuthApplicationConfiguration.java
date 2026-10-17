package com.jnta.auth.adapter.configuration;

import com.jnta.auth.application.AuthenticateUserUseCase;
import com.jnta.auth.application.RegisterClientUseCase;
import com.jnta.auth.application.port.out.AccessTokenIssuer;
import com.jnta.user.application.port.out.PasswordHasher;
import com.jnta.user.application.port.out.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AuthApplicationConfiguration {

    @Bean
    RegisterClientUseCase registerClientUseCase(UserRepository users, PasswordHasher passwordHasher) {
        return new RegisterClientUseCase(users, passwordHasher);
    }

    @Bean
    AuthenticateUserUseCase authenticateUserUseCase(
            UserRepository users,
            PasswordHasher passwordHasher,
            AccessTokenIssuer accessTokenIssuer) {
        return new AuthenticateUserUseCase(users, passwordHasher, accessTokenIssuer);
    }

}
