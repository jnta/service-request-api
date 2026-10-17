package com.jnta.error.adapter.in.web;

import java.time.Instant;
import java.util.List;

public record ApiError(String code, String message, List<ApiFieldError> errors, Instant timestamp) {

    public ApiError {
        errors = List.copyOf(errors);
    }
}
