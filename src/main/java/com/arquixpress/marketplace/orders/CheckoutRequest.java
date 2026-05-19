package com.arquixpress.marketplace.orders;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record CheckoutRequest(@NotEmpty List<@Valid CheckoutItemRequest> items, String paymentMethod,
                              UUID deliveryAddressId) {
    public CheckoutRequest(List<@Valid CheckoutItemRequest> items, String paymentMethod) {
        this(items, paymentMethod, null);
    }
}
