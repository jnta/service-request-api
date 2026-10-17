package com.jnta.solicitation.application.command;

public record UpdateAddressCommand(String cep, String number, String complement) {
}
