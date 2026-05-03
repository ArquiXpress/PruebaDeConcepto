package com.arquixpress.marketplace.identity;

import java.util.List;
import java.util.UUID;

public record LoginResponse(
        UUID id,
        String email,
        String displayName,
        List<Role> roles,
        String avatarUrl,
        String phone,
        String address,
        String city,
        String documentNumber) {
    public static LoginResponse from(AuthUser user) {
        return new LoginResponse(
                user.id(),
                user.email(),
                user.displayName(),
                List.copyOf(user.roles()),
                user.avatarUrl(),
                user.phone(),
                user.address(),
                user.city(),
                user.documentNumber());
    }
}
