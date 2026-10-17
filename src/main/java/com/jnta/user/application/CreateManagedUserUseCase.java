package com.jnta.user.application;

import com.jnta.shared.identity.UserId;
import com.jnta.user.application.command.CreateManagedUserCommand;
import com.jnta.user.application.exception.EmailAlreadyRegisteredException;
import com.jnta.user.application.port.out.PasswordHasher;
import com.jnta.user.application.port.out.UserRepository;
import com.jnta.user.domain.EmailAddress;
import com.jnta.user.domain.User;

public class CreateManagedUserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public CreateManagedUserUseCase(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    public User create(CreateManagedUserCommand command) {
        var email = new EmailAddress(command.email());
        if (userRepository.findByEmail(email).isPresent()) {
            throw new EmailAlreadyRegisteredException(email);
        }
        var user = User.createManaged(
                UserId.generate(),
                command.name(),
                email,
                passwordHasher.hash(command.password()),
                command.role(),
                command.coverage());
        return userRepository.save(user);
    }
}
