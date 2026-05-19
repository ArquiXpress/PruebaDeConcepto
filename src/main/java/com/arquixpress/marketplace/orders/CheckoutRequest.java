package com.arquixpress.marketplace.orders;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CheckoutRequest(
        @NotEmpty List<@Valid CheckoutItemRequest> items,
        String paymentMethod,
        String shippingAddress,
        String shippingCity) {
}
