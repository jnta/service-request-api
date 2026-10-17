package com.jnta.user.adapter.out.persistence;

import com.jnta.user.application.exception.EmailAlreadyRegisteredException;
import com.jnta.user.application.port.out.UserRepository;
import com.jnta.user.domain.EmailAddress;
import com.jnta.user.domain.User;
import com.jnta.shared.identity.UserId;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class JpaUserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository repository;
    private final UserJpaMapper mapper = new UserJpaMapper();

    @Override
    @Transactional
    public User save(User user) {
        try {
            return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(user)));
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyRegisteredException(user.email());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(UserId id) {
        return repository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(EmailAddress email) {
        return repository.findByEmail(email.value()).map(mapper::toDomain);
    }
}
