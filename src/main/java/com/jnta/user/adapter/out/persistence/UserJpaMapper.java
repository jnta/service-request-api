package com.jnta.user.adapter.out.persistence;

import com.jnta.shared.identity.UserId;
import com.jnta.user.domain.EmailAddress;
import com.jnta.user.domain.PasswordHash;
import com.jnta.user.domain.User;
import java.util.LinkedHashSet;

final class UserJpaMapper {

    UserJpaEntity toEntity(User user) {
        var entity = new UserJpaEntity();
        entity.id = user.id().value();
        entity.name = user.name();
        entity.email = user.email().value();
        entity.passwordHash = user.passwordHash().value();
        entity.role = user.role();
        entity.coverage = new LinkedHashSet<>(user.coverage());
        return entity;
    }

    User toDomain(UserJpaEntity entity) {
        return new User(
                new UserId(entity.id),
                entity.name,
                new EmailAddress(entity.email),
                new PasswordHash(entity.passwordHash),
                entity.role,
                entity.coverage);
    }
}
