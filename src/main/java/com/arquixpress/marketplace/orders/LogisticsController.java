package com.arquixpress.marketplace.orders;

import com.arquixpress.marketplace.identity.AppUser;
import com.arquixpress.marketplace.identity.AppUserRepository;
import com.arquixpress.marketplace.identity.CurrentUser;
import com.arquixpress.marketplace.identity.Role;
import com.arquixpress.marketplace.identity.RoleGuard;
import com.arquixpress.marketplace.logistics.LogisticsCenter;
import com.arquixpress.marketplace.logistics.LogisticsCenterRepository;
import com.arquixpress.marketplace.logistics.LogisticsOperator;
import com.arquixpress.marketplace.logistics.LogisticsOperatorRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/logistics")
class LogisticsController {
    private final CheckoutService checkout;
    private final RoleGuard roles;
    private final AppUserRepository users;
    private final LogisticsCenterRepository centers;
    private final LogisticsOperatorRepository operators;

    LogisticsController(CheckoutService checkout, RoleGuard roles, AppUserRepository users,
            LogisticsCenterRepository centers, LogisticsOperatorRepository operators) {
        this.checkout = checkout;
        this.roles = roles;
        this.users = users;
        this.centers = centers;
        this.operators = operators;
    }

    @PatchMapping("/orders/{orderId}/shipment")
    OrderResponse updateShipment(@PathVariable UUID orderId, @Valid @RequestBody ShipmentUpdateRequest request,
            HttpServletRequest http) {
        CurrentUser current = CurrentUser.from(http);
        roles.requireAny(current, Role.LOGISTICS, Role.ADMIN, Role.SUPERADMIN);
        return checkout.updateShipment(orderId, request.status());
    }

    @GetMapping("/me")
    Map<String, Object> me(HttpServletRequest http) {
        CurrentUser current = CurrentUser.from(http);
        roles.requireAny(current, Role.LOGISTICS, Role.ADMIN, Role.SUPERADMIN);
        Map<String, Object> result = new HashMap<>();
        result.put("userId", current.id());
        AppUser user = users.findById(current.id()).orElse(null);
        if (user != null) {
            result.put("city", user.city());
        }
        operators.findByAppUserId(current.id()).ifPresent(op -> {
            result.put("operatorId", op.id());
            result.put("centerId", op.center().id());
            result.put("centerCity", op.center().city());
            result.put("centerName", op.center().displayName());
        });
        return result;
    }

    @GetMapping("/centers")
    List<LogisticsCenter> listCenters(HttpServletRequest http) {
        roles.requireAny(CurrentUser.from(http), Role.LOGISTICS, Role.ADMIN, Role.SUPERADMIN);
        return centers.findAll();
    }

    @GetMapping("/orders")
    List<OrderResponse> listOrders(HttpServletRequest http) {
        CurrentUser current = CurrentUser.from(http);
        roles.requireAny(current, Role.LOGISTICS, Role.ADMIN, Role.SUPERADMIN);
        if (current.hasAny(Role.ADMIN, Role.SUPERADMIN)) {
            return checkout.listShipmentsByCenter(null);
        }
        LogisticsOperator op = operators.findByAppUserId(current.id()).orElse(null);
        if (op == null) {
            return List.of();
        }
        return checkout.listShipmentsByCenter(op.center().id());
    }
}
