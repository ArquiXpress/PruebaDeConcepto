package com.arquixpress.marketplace.sellers;

import com.arquixpress.marketplace.identity.CurrentUser;
import com.arquixpress.marketplace.identity.Role;
import com.arquixpress.marketplace.identity.RoleGuard;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seller-applications")
public class SellerApplicationController {
    private final RoleGuard roles;
    private final SellerApplicationService service;

    public SellerApplicationController(RoleGuard roles, SellerApplicationService service) {
        this.roles = roles;
        this.service = service;
    }

    @PostMapping
    public SellerApplicationResponse create(@Valid @RequestBody SellerApplicationRequest request, HttpServletRequest http) {
        CurrentUser user = CurrentUser.from(http);
        roles.requireAny(user, Role.CLIENT);
        return service.create(user, request);
    }
}
