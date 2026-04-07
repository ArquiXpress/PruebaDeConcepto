package com.arquixpress.marketplace.identity;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record CurrentUser(UUID id, Set<Role> roles) {
    public static CurrentUser from(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        String rolesHeader = request.getHeader("X-Roles");
        UUID id = userId == null || userId.isBlank()
                ? UUID.fromString("00000000-0000-0000-0000-000000000001")
                : UUID.fromString(userId);
        Set<Role> roles = rolesHeader == null || rolesHeader.isBlank()
                ? Set.of(Role.CLIENT)
                : Arrays.stream(rolesHeader.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .map(String::toUpperCase)
                    .map(Role::valueOf)
                    .collect(Collectors.toSet());
        return new CurrentUser(id, roles);
    }

    public boolean hasAny(Role... expected) {
        return Arrays.stream(expected).anyMatch(roles::contains);
    }
}
