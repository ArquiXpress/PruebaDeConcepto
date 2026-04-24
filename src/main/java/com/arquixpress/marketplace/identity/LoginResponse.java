package com.arquixpress.marketplace.identity;

import java.util.List;
import java.util.UUID;

public record LoginResponse(UUID id, String email, String displayName, List<Role> roles) {
    public static LoginResponse from(AuthUser user) {
        return new LoginResponse(user.id(), user.email(), user.displayName(), List.copyOf(user.roles()));
    }
}
