package com.jnta.error.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.jnta.solicitation.domain.exception.IncompleteSolicitationException;
import com.jnta.solicitation.domain.validation.Violation;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.MapBindingResult;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.context.request.WebRequest;

class GlobalExceptionHandlerTest {

    private static final Instant NOW = Instant.parse("2026-10-17T12:00:00Z");

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void shouldMapIncompleteSolicitationTo422WithFieldViolations() {
        var exception = new IncompleteSolicitationException(List.of(
                new Violation("address", "Address is required"),
                new Violation("termsAccepted", "Terms must be accepted")));

        var response = handler.handleIncomplete(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isEqualTo(new ApiError(
                "SOLICITATION_INCOMPLETE",
                "Solicitation cannot be submitted",
                List.of(
                        new ApiFieldError("address", "Address is required"),
                        new ApiFieldError("termsAccepted", "Terms must be accepted")),
                NOW));
    }

    @Test
    void shouldMapBeanValidationFailuresToFieldErrors() throws Exception {
        var bindingResult = new MapBindingResult(new HashMap<>(), "request");
        bindingResult.rejectValue("title", "NotBlank", "must not be blank");
        var exception = new BindException(bindingResult);

        var error = handler.validationError(exception);

        assertThat(error).isEqualTo(new ApiError(
                "VALIDATION_ERROR",
                "Request validation failed",
                List.of(new ApiFieldError("title", "must not be blank")),
                NOW));
    }

    @Test
    void shouldMapMalformedRequestBodiesTo400() {
        var response = handler.handleHttpMessageNotReadable(
                new HttpMessageNotReadableException("Malformed request", (org.springframework.http.HttpInputMessage) null),
                new HttpHeaders(),
                HttpStatus.BAD_REQUEST,
                mock(WebRequest.class));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(new ApiError(
                "MALFORMED_REQUEST", "Request body is malformed or unreadable", List.of(), NOW));
    }

    @Test
    void shouldNotLeakExceptionDetailsForUnexpectedFailures() throws Exception {
        var exception = new IllegalStateException("database password leaked here");

        var response = handler.createResponseEntity(
                null,
                new HttpHeaders(),
                HttpStatus.INTERNAL_SERVER_ERROR,
                mock(WebRequest.class));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo(new ApiError(
                "INTERNAL_ERROR", "An unexpected error occurred", List.of(), NOW));
    }
}
