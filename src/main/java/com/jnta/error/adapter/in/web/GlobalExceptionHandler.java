package com.jnta.error.adapter.in.web;

import com.jnta.auth.application.exception.InvalidCredentialsException;
import com.jnta.search.application.InvalidSearchQueryException;
import com.jnta.search.application.SearchAccessDeniedException;
import com.jnta.solicitation.application.exception.ConcurrentSolicitationModificationException;
import com.jnta.solicitation.application.exception.PostalCodeNotFoundException;
import com.jnta.solicitation.application.exception.PostalCodeUnavailableException;
import com.jnta.solicitation.application.exception.SolicitationAccessDeniedException;
import com.jnta.solicitation.application.exception.SolicitationNotFoundException;
import com.jnta.solicitation.domain.exception.IncompleteSolicitationException;
import com.jnta.solicitation.domain.exception.InvalidSolicitationDataException;
import com.jnta.solicitation.domain.exception.InvalidSolicitationStateException;
import com.jnta.user.application.exception.EmailAlreadyRegisteredException;
import com.jnta.user.domain.exception.InvalidUserDataException;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(SolicitationNotFoundException.class)
    ResponseEntity<ApiError> handleNotFound() {
        return response(HttpStatus.NOT_FOUND, "SOLICITATION_NOT_FOUND", "Solicitation was not found");
    }

    @ExceptionHandler({SolicitationAccessDeniedException.class, SearchAccessDeniedException.class})
    ResponseEntity<ApiError> handleAccessDenied() {
        return response(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Access to this resource is forbidden");
    }

    @ExceptionHandler({InvalidSolicitationStateException.class, ConcurrentSolicitationModificationException.class})
    ResponseEntity<ApiError> handleConflict() {
        return response(HttpStatus.CONFLICT, "STATE_CONFLICT", "The resource state has changed");
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    ResponseEntity<ApiError> handleDuplicateEmail() {
        return response(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "Email is already registered");
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ApiError> handleInvalidCredentials() {
        return response(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password");
    }

    @ExceptionHandler(InvalidUserDataException.class)
    ResponseEntity<ApiError> handleInvalidUserData(InvalidUserDataException exception) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_USER_DATA", exception.getMessage());
    }

    @ExceptionHandler(InvalidSearchQueryException.class)
    ResponseEntity<ApiError> handleInvalidSearchQuery(InvalidSearchQueryException exception) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_SEARCH_QUERY", exception.getMessage());
    }

    @ExceptionHandler(IncompleteSolicitationException.class)
    ResponseEntity<ApiError> handleIncomplete(IncompleteSolicitationException exception) {
        var errors = exception.violations().stream()
                .map(violation -> new ApiFieldError(violation.field(), violation.message()))
                .toList();
        return response(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "SOLICITATION_INCOMPLETE",
                "Solicitation cannot be submitted",
                errors);
    }

    @ExceptionHandler(PostalCodeNotFoundException.class)
    ResponseEntity<ApiError> handlePostalCodeNotFound() {
        return response(HttpStatus.UNPROCESSABLE_ENTITY, "POSTAL_CODE_NOT_FOUND", "Postal code was not found");
    }

    @ExceptionHandler(PostalCodeUnavailableException.class)
    ResponseEntity<ApiError> handlePostalCodeUnavailable() {
        return response(
                HttpStatus.SERVICE_UNAVAILABLE,
                "POSTAL_CODE_SERVICE_UNAVAILABLE",
                "Postal code service is unavailable");
    }

    @ExceptionHandler(InvalidSolicitationDataException.class)
    ResponseEntity<ApiError> handleInvalidSolicitationData(InvalidSolicitationDataException exception) {
        return response(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_SOLICITATION_DATA", exception.getMessage());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        return createResponseEntity(validationError(exception), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        return createResponseEntity(
                error("MALFORMED_REQUEST", "Request body is malformed or unreadable", List.of()),
                headers,
                status,
                request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        return createResponseEntity(
                validationError(exception),
                headers,
                status,
                request);
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(
            Object body,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        Object apiError = body instanceof ApiError ? body : frameworkError(status);
        return super.createResponseEntity(apiError, headers, status, request);
    }

    ApiError validationError(BindException exception) {
        var errors = exception.getBindingResult().getFieldErrors().stream()
                .map(this::fieldError)
                .toList();
        return validationError(errors);
    }

    private ApiError validationError(HandlerMethodValidationException exception) {
        var errors = exception.getParameterValidationResults().stream()
                .flatMap(result -> {
                    if (result instanceof org.springframework.validation.method.ParameterErrors parameterErrors) {
                        return parameterErrors.getFieldErrors().stream().map(this::fieldError);
                    }
                    var parameterName = result.getMethodParameter().getParameterName();
                    var field = parameterName == null ? "request" : parameterName;
                    return result.getResolvableErrors().stream()
                            .map(error -> new ApiFieldError(field, error.getDefaultMessage() == null
                                    ? "Invalid value"
                                    : error.getDefaultMessage()));
                })
                .toList();
        return validationError(errors);
    }

    private ApiError validationError(List<ApiFieldError> errors) {
        var sortedErrors = errors.stream()
                .sorted(Comparator.comparing(ApiFieldError::field).thenComparing(ApiFieldError::message))
                .toList();
        return error("VALIDATION_ERROR", "Request validation failed", sortedErrors);
    }

    private ApiError frameworkError(HttpStatusCode status) {
        if (status.value() == HttpStatus.UNAUTHORIZED.value()) {
            return error("UNAUTHENTICATED", "Authentication is required", List.of());
        }
        if (status.value() == HttpStatus.FORBIDDEN.value()) {
            return error("ACCESS_DENIED", "Access to this resource is forbidden", List.of());
        }
        if (status.value() == HttpStatus.NOT_FOUND.value()) {
            return error("RESOURCE_NOT_FOUND", "The requested resource was not found", List.of());
        }
        if (status.value() == HttpStatus.CONFLICT.value()) {
            return error("STATE_CONFLICT", "The resource state has changed", List.of());
        }
        if (status.is4xxClientError()) {
            return error("INVALID_REQUEST", "The request could not be processed", List.of());
        }
        return error("INTERNAL_ERROR", "An unexpected error occurred", List.of());
    }

    private ApiFieldError fieldError(FieldError error) {
        return new ApiFieldError(error.getField(), error.getDefaultMessage() == null
                ? "Invalid value"
                : error.getDefaultMessage());
    }

    private ResponseEntity<ApiError> response(HttpStatus status, String code, String message) {
        return response(status, code, message, List.of());
    }

    private ResponseEntity<ApiError> response(
            HttpStatus status, String code, String message, List<ApiFieldError> errors) {
        return ResponseEntity.status(status).body(error(code, message, errors));
    }

    private ApiError error(String code, String message, List<ApiFieldError> errors) {
        return new ApiError(code, message, errors, clock.instant());
    }
}
