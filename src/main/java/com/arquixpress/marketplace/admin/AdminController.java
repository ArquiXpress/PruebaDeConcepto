package com.arquixpress.marketplace.admin;

import com.arquixpress.marketplace.identity.CurrentUser;
import com.arquixpress.marketplace.identity.Role;
import com.arquixpress.marketplace.identity.RoleGuard;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final RoleGuard roles;
    private final RollbackService rollbackService;
    private final AdminStatsService statsService;
    private final AdminUserService userService;

    public AdminController(RoleGuard roles, RollbackService rollbackService, AdminStatsService statsService,
            AdminUserService userService) {
        this.roles = roles;
        this.rollbackService = rollbackService;
        this.statsService = statsService;
        this.userService = userService;
    }

    @GetMapping("/stats")
    public AdminStatsResponse getStats(HttpServletRequest http) {
        roles.requireAny(CurrentUser.from(http), Role.ADMIN, Role.SUPERADMIN);
        return statsService.getStats();
    }

    @GetMapping("/users")
    public List<AdminUserResponse> getAllUsers(HttpServletRequest http) {
        roles.requireAny(CurrentUser.from(http), Role.ADMIN, Role.SUPERADMIN);
        return userService.getAllUsers();
    }

    @PostMapping("/users/{userId}/make-admin")
    public AdminUserResponse makeUserAdmin(@PathVariable UUID userId, HttpServletRequest http) {
        roles.requireAny(CurrentUser.from(http), Role.ADMIN, Role.SUPERADMIN);
        return userService.makeAdmin(userId);
    }

    @PutMapping("/users/{userId}/roles")
    public AdminUserResponse updateUserRoles(@PathVariable UUID userId, @RequestBody UpdateUserRolesRequest request,
            HttpServletRequest http) {
        roles.requireAny(CurrentUser.from(http), Role.ADMIN, Role.SUPERADMIN);
        return userService.updateUserRoles(userId, request.roles());
    }

    @PostMapping("/rollback")
    public RollbackResponse rollback(HttpServletRequest http) {
        roles.requireAny(CurrentUser.from(http), Role.ADMIN, Role.SUPERADMIN);
        rollbackService.resetDatabase();
        return new RollbackResponse(true, "Base de datos restaurada al estado inicial");
    }
}
