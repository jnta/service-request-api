package com.jnta.auth.adapter.in.web;

import com.jnta.auth.application.exception.InvalidCredentialsException;
import com.jnta.user.application.exception.EmailAlreadyRegisteredException;
import com.jnta.user.adapter.in.web.AdminUserController;
import com.jnta.user.domain.exception.InvalidUserDataException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = {AuthController.class, AdminUserController.class})
public class AuthenticationExceptionHandler {

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    ResponseEntity<ProblemDetail> handleDuplicateEmail() {
        return problem(HttpStatus.CONFLICT, "Email is already registered");
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ProblemDetail> handleInvalidCredentials() {
        return problem(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    @ExceptionHandler(InvalidUserDataException.class)
    ResponseEntity<ProblemDetail> handleInvalidUserData(InvalidUserDataException exception) {
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
    }
}
