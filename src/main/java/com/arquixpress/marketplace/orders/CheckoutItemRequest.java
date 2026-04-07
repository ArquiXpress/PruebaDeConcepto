package com.arquixpress.marketplace.orders;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CheckoutItemRequest(@NotNull UUID productId, @Min(1) int quantity) {
}
