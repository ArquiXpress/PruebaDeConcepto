package com.arquixpress.marketplace.orders;

import org.springframework.http.HttpStatus;

public class CheckoutProblem extends RuntimeException {
    private final String code;
    private final HttpStatus status;

    public CheckoutProblem(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String code() { return code; }
    public HttpStatus status() { return status; }
}
