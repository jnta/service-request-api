package com.jnta.solicitation.adapter.out.viacep;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
public class ViaCepConfiguration {

    @Bean
    RestClient viaCepRestClient(@Value("${integration.viacep.base-url}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }
}
