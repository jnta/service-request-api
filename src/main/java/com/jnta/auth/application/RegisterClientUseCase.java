package com.jnta.auth.application;

import com.jnta.auth.application.command.RegisterClientCommand;
import com.jnta.shared.identity.UserId;
import com.jnta.user.application.exception.EmailAlreadyRegisteredException;
import com.jnta.user.application.port.out.PasswordHasher;
import com.jnta.user.application.port.out.UserRepository;
import com.jnta.user.domain.EmailAddress;
import com.jnta.user.domain.User;

public class RegisterClientUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public RegisterClientUseCase(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    public User register(RegisterClientCommand command) {
        var email = new EmailAddress(command.email());
        if (userRepository.findByEmail(email).isPresent()) {
            throw new EmailAlreadyRegisteredException(email);
        }
        var user = User.registerClient(
                UserId.generate(), command.name(), email, passwordHasher.hash(command.password()));
        return userRepository.save(user);
    }
}
