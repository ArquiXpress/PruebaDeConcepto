package com.arquixpress.marketplace.identity;

import java.util.Set;
import java.util.UUID;

public record AuthUser(
        UUID id,
        String email,
        String displayName,
        Set<Role> roles,
        String avatarUrl,
        String phone,
        String address,
        String city,
        String documentNumber) {
}
