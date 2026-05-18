package com.arquixpress.marketplace.promotions;

import com.arquixpress.marketplace.identity.CurrentUser;
import com.arquixpress.marketplace.identity.Role;
import com.arquixpress.marketplace.identity.RoleGuard;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/promotions")
class PromotionController {
    private final RoleGuard roles;

    PromotionController(RoleGuard roles) {
        this.roles = roles;
    }

    @PostMapping
    PromotionResponse create(@Valid @RequestBody PromotionRequest request, HttpServletRequest http) {
        roles.requireAny(CurrentUser.from(http), Role.ADMIN);
        if (request.endsAt().isBefore(request.startsAt())) {
            throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la fecha de inicio");
        }
        return new PromotionResponse(UUID.randomUUID(), request.name(), request.startsAt(), request.endsAt(), "ACTIVE_MOCK");
    }
}
