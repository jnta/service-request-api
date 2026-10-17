package com.jnta.solicitation.application.port.out;

import com.jnta.solicitation.domain.address.Cep;

public interface PostalCodeGateway {

    PostalCodeResult find(Cep cep);
}
