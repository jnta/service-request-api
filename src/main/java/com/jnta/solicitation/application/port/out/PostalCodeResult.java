package com.jnta.solicitation.application.port.out;

import com.jnta.shared.geography.BrazilianState;

public record PostalCodeResult(String street, String neighborhood, String city, BrazilianState state) {
}
