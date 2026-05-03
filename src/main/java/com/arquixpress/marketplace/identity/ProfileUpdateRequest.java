package com.arquixpress.marketplace.identity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ProfileUpdateRequest(
        @NotBlank String displayName,
        @NotBlank @Email String email,
        String avatarUrl,
        String phone,
        String address,
        String city,
        String documentNumber) {
}
