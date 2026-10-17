package com.jnta.solicitation.adapter.out.viacep;

import com.jnta.shared.geography.BrazilianState;
import com.jnta.solicitation.application.exception.PostalCodeNotFoundException;
import com.jnta.solicitation.application.exception.PostalCodeUnavailableException;
import com.jnta.solicitation.application.port.out.PostalCodeGateway;
import com.jnta.solicitation.application.port.out.PostalCodeResult;
import com.jnta.solicitation.domain.address.Cep;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
public class ViaCepClient implements PostalCodeGateway {

    private final RestClient viaCepRestClient;

    @Override
    public PostalCodeResult find(Cep cep) {
        ViaCepResponse response;
        try {
            response = viaCepRestClient.get()
                    .uri("/{cep}/json/", cep.value())
                    .retrieve()
                    .body(ViaCepResponse.class);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                throw new PostalCodeNotFoundException(cep);
            }
            throw new PostalCodeUnavailableException("ViaCEP request failed", exception);
        } catch (RestClientException exception) {
            throw new PostalCodeUnavailableException("ViaCEP is unavailable", exception);
        }

        if (response == null) {
            throw new PostalCodeUnavailableException("ViaCEP returned an empty response");
        }
        if (Boolean.TRUE.equals(response.erro())) {
            throw new PostalCodeNotFoundException(cep);
        }

        return toPostalCodeResult(response);
    }

    private PostalCodeResult toPostalCodeResult(ViaCepResponse response) {
        if (isBlank(response.logradouro()) || isBlank(response.bairro())
                || isBlank(response.localidade()) || isBlank(response.uf())) {
            throw new PostalCodeUnavailableException("ViaCEP returned incomplete address data");
        }
        try {
            var state = BrazilianState.valueOf(response.uf().trim().toUpperCase(Locale.ROOT));
            return new PostalCodeResult(response.logradouro(), response.bairro(), response.localidade(), state);
        } catch (IllegalArgumentException exception) {
            throw new PostalCodeUnavailableException("ViaCEP returned an unsupported state code", exception);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
