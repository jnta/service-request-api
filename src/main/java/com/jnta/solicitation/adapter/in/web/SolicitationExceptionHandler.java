package com.jnta.solicitation.adapter.in.web;

import com.jnta.solicitation.application.exception.SolicitationAccessDeniedException;
import com.jnta.solicitation.application.exception.SolicitationNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = SolicitationController.class)
public class SolicitationExceptionHandler {

    @ExceptionHandler(SolicitationNotFoundException.class)
    ResponseEntity<ProblemDetail> handleNotFound() {
        return problem(HttpStatus.NOT_FOUND, "Solicitation was not found");
    }

    @ExceptionHandler(SolicitationAccessDeniedException.class)
    ResponseEntity<ProblemDetail> handleAccessDenied() {
        return problem(HttpStatus.FORBIDDEN, "Access to this solicitation is forbidden");
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
    }
}
