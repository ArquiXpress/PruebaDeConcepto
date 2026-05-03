package com.arquixpress.marketplace.identity;

import java.time.Instant;

public record PasswordResetResponse(
        boolean accepted,
        Instant expiresAt,
        String message) {
}
