package com.arquixpress.marketplace.admin;

import com.arquixpress.marketplace.identity.CurrentUser;
import com.arquixpress.marketplace.identity.Role;
import com.arquixpress.marketplace.identity.RoleGuard;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final RoleGuard roles;
    private final RollbackService rollbackService;

    public AdminController(RoleGuard roles, RollbackService rollbackService) {
        this.roles = roles;
        this.rollbackService = rollbackService;
    }

    @PostMapping("/rollback")
    public RollbackResponse rollback(HttpServletRequest http) {
        roles.requireAny(CurrentUser.from(http), Role.ADMIN, Role.SUPERADMIN);
        rollbackService.resetDatabase();
        return new RollbackResponse(true, "Base de datos restaurada al estado inicial");
    }
}
