package com.arquixpress.marketplace.orders;

import com.arquixpress.marketplace.identity.CurrentUser;
import com.arquixpress.marketplace.identity.Role;
import com.arquixpress.marketplace.identity.RoleGuard;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class CheckoutController {
    private final CheckoutService checkout;
    private final RoleGuard roles;

    CheckoutController(CheckoutService checkout, RoleGuard roles) {
        this.checkout = checkout;
        this.roles = roles;
    }

    @PostMapping("/checkout")
    CheckoutResponse checkout(@Valid @RequestBody CheckoutRequest request,
                              @RequestHeader("Idempotency-Key") String idempotencyKey,
                              HttpServletRequest http) {
        CurrentUser user = CurrentUser.from(http);
        roles.requireAny(user, Role.CLIENT);
        return checkout.checkout(user.id(), request, idempotencyKey);
    }

    @PostMapping("/payments/{orderId}/retry")
    CheckoutResponse retryPayment(@PathVariable UUID orderId,
                                  @RequestHeader("Idempotency-Key") String idempotencyKey,
                                  HttpServletRequest http) {
        CurrentUser user = CurrentUser.from(http);
        roles.requireAny(user, Role.CLIENT);
        return checkout.retryPayment(orderId, idempotencyKey);
    }

    @GetMapping("/orders/{orderId}")
    OrderResponse order(@PathVariable UUID orderId, HttpServletRequest http) {
        roles.requireAny(CurrentUser.from(http), Role.CLIENT, Role.SELLER, Role.ADMIN, Role.LOGISTICS);
        return checkout.getOrder(orderId);
    }
}
