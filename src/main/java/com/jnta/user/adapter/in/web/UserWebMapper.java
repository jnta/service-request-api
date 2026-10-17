package com.jnta.user.adapter.in.web;

import com.jnta.shared.mapping.StrictMappingConfig;
import com.jnta.user.application.command.CreateManagedUserCommand;
import com.jnta.user.domain.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = StrictMappingConfig.class)
public interface UserWebMapper {

    CreateManagedUserCommand toCommand(CreateUserRequest request);

    @BeanMapping(ignoreUnmappedSourceProperties = "passwordHash")
    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "email", source = "email.value")
    UserResponse toResponse(User user);
}
