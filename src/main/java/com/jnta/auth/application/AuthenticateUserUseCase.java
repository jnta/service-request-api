package com.jnta.auth.application;

import com.jnta.auth.application.command.AuthenticateUserCommand;
import com.jnta.auth.application.exception.InvalidCredentialsException;
import com.jnta.auth.application.port.out.AccessTokenIssuer;
import com.jnta.user.application.port.out.UserRepository;
import com.jnta.user.application.port.out.PasswordHasher;
import com.jnta.user.domain.EmailAddress;

public class AuthenticateUserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final AccessTokenIssuer accessTokenIssuer;

    public AuthenticateUserUseCase(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            AccessTokenIssuer accessTokenIssuer) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.accessTokenIssuer = accessTokenIssuer;
    }

    public String authenticate(AuthenticateUserCommand command) {
        var user = userRepository.findByEmail(new EmailAddress(command.email()))
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordHasher.matches(command.password(), user.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        return accessTokenIssuer.issue(user);
    }
}
