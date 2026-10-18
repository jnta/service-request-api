package com.jnta.solicitation.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateBasicDataRequest(
        @NotBlank @Pattern(regexp = "(?i)INSTALLATION|MAINTENANCE|INSPECTION") String serviceType,
        @NotBlank String title,
        @NotBlank String description) {
}
