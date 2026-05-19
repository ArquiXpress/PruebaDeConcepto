package com.arquixpress.marketplace.orders;

import com.arquixpress.marketplace.identity.CurrentUser;
import com.arquixpress.marketplace.identity.Role;
import com.arquixpress.marketplace.identity.RoleGuard;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seller/orders")
class SellerOrderController {
    private final SellerOrderService sellerOrders;
    private final RoleGuard roles;

    SellerOrderController(SellerOrderService sellerOrders, RoleGuard roles) {
        this.sellerOrders = sellerOrders;
        this.roles = roles;
    }

    @GetMapping
    List<OrderResponse> list(HttpServletRequest http) {
        CurrentUser user = CurrentUser.from(http);
        roles.requireAny(user, Role.SELLER, Role.ADMIN, Role.SUPERADMIN);
        return sellerOrders.listForSeller(user.id());
    }
}
