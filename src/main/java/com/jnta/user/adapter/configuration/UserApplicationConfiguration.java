package com.jnta.user.adapter.configuration;

import com.jnta.user.application.CreateManagedUserUseCase;
import com.jnta.user.application.port.out.PasswordHasher;
import com.jnta.user.application.port.out.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class UserApplicationConfiguration {

    @Bean
    CreateManagedUserUseCase createManagedUserUseCase(UserRepository users, PasswordHasher passwordHasher) {
        return new CreateManagedUserUseCase(users, passwordHasher);
    }
}
