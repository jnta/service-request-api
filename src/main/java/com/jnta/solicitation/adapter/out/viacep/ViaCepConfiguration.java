package com.jnta.solicitation.adapter.out.viacep;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
public class ViaCepConfiguration {

    @Bean
    RestClient viaCepRestClient(
            @Value("${integration.viacep.base-url}") String baseUrl,
            @Value("${integration.viacep.connect-timeout}") Duration connectTimeout,
            @Value("${integration.viacep.read-timeout}") Duration readTimeout) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
