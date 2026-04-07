package com.arquixpress.marketplace.orders;

import com.arquixpress.marketplace.identity.CurrentUser;
import com.arquixpress.marketplace.identity.Role;
import com.arquixpress.marketplace.identity.RoleGuard;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
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

    LogisticsController(CheckoutService checkout, RoleGuard roles) {
        this.checkout = checkout;
        this.roles = roles;
    }

    @PatchMapping("/orders/{orderId}/shipment")
    OrderResponse updateShipment(@PathVariable UUID orderId, @Valid @RequestBody ShipmentUpdateRequest request, HttpServletRequest http) {
        roles.requireAny(CurrentUser.from(http), Role.LOGISTICS, Role.ADMIN);
        return checkout.updateShipment(orderId, request.status());
    }
}
