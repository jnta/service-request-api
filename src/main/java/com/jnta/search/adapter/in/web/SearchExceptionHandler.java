package com.jnta.search.adapter.in.web;

import com.jnta.search.application.InvalidSearchQueryException;
import com.jnta.search.application.SearchAccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = SearchSolicitationsController.class)
public class SearchExceptionHandler {

    @ExceptionHandler(SearchAccessDeniedException.class)
    ResponseEntity<ProblemDetail> handleAccessDenied(SearchAccessDeniedException exception) {
        return problem(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(InvalidSearchQueryException.class)
    ResponseEntity<ProblemDetail> handleInvalidQuery(InvalidSearchQueryException exception) {
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
    }
}
