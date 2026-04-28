package com.arquixpress.marketplace.admin;

public record RollbackResponse(
    boolean success,
    String message
) {}
