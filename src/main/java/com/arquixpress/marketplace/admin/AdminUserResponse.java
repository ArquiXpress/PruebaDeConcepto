package com.arquixpress.marketplace.admin;

import java.util.UUID;
import com.arquixpress.marketplace.identity.UserStatus;

public record AdminUserResponse(
    UUID id,
    String email,
    String displayName,
    String roles,
    UserStatus status
) {}
