package com.jnta.auth;

import com.jnta.error.adapter.in.web.ApiErrorWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            ApiErrorWriter apiErrorWriter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint((request, response, exception) -> apiErrorWriter.write(
                                response,
                                HttpStatus.UNAUTHORIZED,
                                "UNAUTHENTICATED",
                                "Authentication is required"))
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> apiErrorWriter.write(
                                response,
                                HttpStatus.UNAUTHORIZED,
                                "UNAUTHENTICATED",
                                "Authentication is required"))
                        .accessDeniedHandler((request, response, exception) -> apiErrorWriter.write(
                                response,
                                HttpStatus.FORBIDDEN,
                                "ACCESS_DENIED",
                                "Access to this resource is forbidden")))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/register", "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/admin/users").hasRole("ADMIN")
                        .requestMatchers("/analyst/solicitations/**").hasAnyRole("ANALYST", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/solicitations").hasRole("CLIENT")
                        .requestMatchers(HttpMethod.PUT, "/solicitations/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/solicitations/**").hasAnyRole("CLIENT", "ADMIN")
                        .anyRequest().authenticated());
        return http.build();
    }
}
