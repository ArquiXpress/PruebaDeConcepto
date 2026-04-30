package com.arquixpress.marketplace.admin;

import java.util.Set;

public record UpdateUserRolesRequest(
    Set<String> roles
) {}
