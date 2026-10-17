package com.jnta.solicitation.adapter.in.web;

import com.jnta.shared.mapping.StrictMappingConfig;
import com.jnta.solicitation.application.command.UpdateBasicDataCommand;
import org.mapstruct.Mapper;

@Mapper(config = StrictMappingConfig.class)
public interface BasicDataRequestMapper {

    UpdateBasicDataCommand toCommand(UpdateBasicDataRequest request);
}
