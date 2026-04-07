package com.arquixpress.marketplace.identity;

import org.springframework.stereotype.Component;

@Component
public class RoleGuard {
    public void requireAny(CurrentUser user, Role... roles) {
        if (!user.hasAny(roles)) {
            throw new AccessDeniedProblem("El rol actual no esta autorizado para esta operacion");
        }
    }
}
