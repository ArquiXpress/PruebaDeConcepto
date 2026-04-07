package com.arquixpress.marketplace.identity;

public class AccessDeniedProblem extends RuntimeException {
    public AccessDeniedProblem(String message) {
        super(message);
    }
}
