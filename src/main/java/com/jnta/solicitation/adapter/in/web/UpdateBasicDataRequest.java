package com.jnta.solicitation.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateBasicDataRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z][A-Za-z0-9_]{0,39}") String serviceType,
        @NotBlank @Size(min = 3, max = 80) String title,
        @NotBlank @Size(min = 20, max = 1000) String description) {
}
