package com.jnta.user.application.port.out;

import com.jnta.shared.identity.UserId;
import com.jnta.user.domain.EmailAddress;
import com.jnta.user.domain.User;
import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(UserId id);

    Optional<User> findByEmail(EmailAddress email);
}
