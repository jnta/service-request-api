package com.jnta.solicitation.adapter.in.web;

import com.jnta.solicitation.application.exception.SolicitationAccessDeniedException;
import com.jnta.solicitation.application.exception.ConcurrentSolicitationModificationException;
import com.jnta.solicitation.application.exception.PostalCodeNotFoundException;
import com.jnta.solicitation.application.exception.PostalCodeUnavailableException;
import com.jnta.solicitation.application.exception.SolicitationNotFoundException;
import com.jnta.solicitation.domain.exception.IncompleteSolicitationException;
import com.jnta.solicitation.domain.exception.InvalidSolicitationDataException;
import com.jnta.solicitation.domain.exception.InvalidSolicitationStateException;
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

    @ExceptionHandler({InvalidSolicitationStateException.class, ConcurrentSolicitationModificationException.class})
    ResponseEntity<ProblemDetail> handleConflict(RuntimeException exception) {
        return problem(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(IncompleteSolicitationException.class)
    ResponseEntity<ProblemDetail> handleIncomplete(IncompleteSolicitationException exception) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
    }

    @ExceptionHandler(PostalCodeNotFoundException.class)
    ResponseEntity<ProblemDetail> handlePostalCodeNotFound() {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Postal code was not found");
    }

    @ExceptionHandler(PostalCodeUnavailableException.class)
    ResponseEntity<ProblemDetail> handlePostalCodeUnavailable() {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Postal code service is unavailable");
    }

    @ExceptionHandler(InvalidSolicitationDataException.class)
    ResponseEntity<ProblemDetail> handleInvalidData(InvalidSolicitationDataException exception) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
    }
}
