package com.jnta.solicitation.adapter.in.web;

import com.jnta.shared.mapping.StrictMappingConfig;
import com.jnta.solicitation.application.command.DecideSolicitationCommand;
import org.mapstruct.Mapper;

@Mapper(config = StrictMappingConfig.class)
public interface AnalystReviewRequestMapper {

    DecideSolicitationCommand toCommand(DecideSolicitationRequest request);
}
