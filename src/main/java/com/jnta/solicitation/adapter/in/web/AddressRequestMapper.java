package com.jnta.solicitation.adapter.in.web;

import com.jnta.shared.mapping.StrictMappingConfig;
import com.jnta.solicitation.application.command.UpdateAddressCommand;
import org.mapstruct.Mapper;

@Mapper(config = StrictMappingConfig.class)
public interface AddressRequestMapper {

    UpdateAddressCommand toCommand(UpdateAddressRequest request);
}
