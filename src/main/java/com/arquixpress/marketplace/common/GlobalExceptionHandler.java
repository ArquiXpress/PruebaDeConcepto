package com.arquixpress.marketplace.common;

import com.arquixpress.marketplace.identity.AccessDeniedProblem;
import com.arquixpress.marketplace.orders.CheckoutProblem;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(CheckoutProblem.class)
    ResponseEntity<ApiError> checkout(CheckoutProblem ex) {
        return ResponseEntity.status(ex.status()).body(ApiError.of(ex.code(), ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedProblem.class)
    ResponseEntity<ApiError> accessDenied(AccessDeniedProblem ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiError.of("ACCESS_DENIED", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest().body(ApiError.of("VALIDATION_ERROR", "Request invalido"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> illegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ApiError.of("BAD_REQUEST", ex.getMessage()));
    }
}
