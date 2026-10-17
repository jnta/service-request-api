package com.jnta.auth.adapter.in.web;

import com.jnta.auth.application.command.AuthenticateUserCommand;
import com.jnta.auth.application.command.RegisterClientCommand;
import com.jnta.shared.mapping.StrictMappingConfig;
import com.jnta.user.domain.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = StrictMappingConfig.class)
public interface AuthRequestMapper {

    RegisterClientCommand toCommand(RegisterClientRequest request);

    AuthenticateUserCommand toCommand(LoginRequest request);

    @BeanMapping(ignoreUnmappedSourceProperties = {"name", "email", "passwordHash", "coverage"})
    @Mapping(target = "id", source = "id.value")
    RegisteredClientResponse toResponse(User user);
}
