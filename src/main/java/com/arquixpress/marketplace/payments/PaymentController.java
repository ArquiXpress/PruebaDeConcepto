package com.arquixpress.marketplace.payments;

import com.arquixpress.marketplace.identity.CurrentUser;
import com.arquixpress.marketplace.identity.Role;
import com.arquixpress.marketplace.identity.RoleGuard;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
class PaymentController {
    private final PaymentReconciliationService reconciliation;
    private final RoleGuard roles;

    PaymentController(PaymentReconciliationService reconciliation, RoleGuard roles) {
        this.reconciliation = reconciliation;
        this.roles = roles;
    }

    @GetMapping("/{orderId}/status")
    PaymentReconciliationResponse status(@PathVariable UUID orderId, HttpServletRequest http) {
        roles.requireAny(CurrentUser.from(http), Role.CLIENT, Role.ADMIN, Role.SUPERADMIN);
        return reconciliation.consultAndSync(orderId);
    }
}
