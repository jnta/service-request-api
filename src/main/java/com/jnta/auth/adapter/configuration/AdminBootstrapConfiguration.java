package com.jnta.auth.adapter.configuration;

import com.jnta.auth.adapter.out.security.AdminBootstrapProperties;
import com.jnta.shared.identity.UserId;
import com.jnta.user.application.port.out.PasswordHasher;
import com.jnta.user.application.port.out.UserRepository;
import com.jnta.user.domain.EmailAddress;
import com.jnta.user.domain.Role;
import com.jnta.user.domain.User;
import java.util.Set;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AdminBootstrapConfiguration {

    @Bean
    ApplicationRunner bootstrapAdministrator(
            AdminBootstrapProperties properties,
            UserRepository userRepository,
            PasswordHasher passwordHasher) {
        return arguments -> {
            if (!properties.enabled()) {
                return;
            }
            var email = new EmailAddress(properties.email());
            var existingUser = userRepository.findByEmail(email);
            if (existingUser.isPresent()) {
                if (existingUser.get().role() != Role.ADMIN) {
                    throw new IllegalStateException("Bootstrap administrator email belongs to a non-admin user");
                }
                return;
            }
            userRepository.save(User.createManaged(
                    UserId.generate(),
                    properties.name(),
                    email,
                    passwordHasher.hash(properties.password()),
                    Role.ADMIN,
                    Set.of()));
        };
    }
}
