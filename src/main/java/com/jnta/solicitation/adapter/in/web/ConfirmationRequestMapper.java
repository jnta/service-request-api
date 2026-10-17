package com.jnta.solicitation.adapter.in.web;

import com.jnta.shared.mapping.StrictMappingConfig;
import com.jnta.solicitation.application.command.UpdateConfirmationCommand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = StrictMappingConfig.class)
public interface ConfirmationRequestMapper {

    @Mapping(target = "estimatedAmount", source = "estimatedValue.amount")
    @Mapping(target = "currency", source = "estimatedValue.currency")
    UpdateConfirmationCommand toCommand(UpdateConfirmationRequest request);
}
