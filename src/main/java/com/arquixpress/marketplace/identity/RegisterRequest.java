package com.arquixpress.marketplace.identity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotBlank String displayName,
        String phone,
        String address,
        String city,
        String documentNumber,
        String avatarUrl,
        String roles) {
}
