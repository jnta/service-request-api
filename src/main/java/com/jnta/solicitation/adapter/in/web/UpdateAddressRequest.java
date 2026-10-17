package com.jnta.solicitation.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateAddressRequest(
        @NotBlank @Pattern(regexp = "[0-9]{5}-?[0-9]{3}") String cep,
        @NotBlank @Size(max = 20) String number,
        @Size(max = 120) String complement) {
}
