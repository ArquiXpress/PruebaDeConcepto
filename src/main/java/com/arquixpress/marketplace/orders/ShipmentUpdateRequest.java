package com.arquixpress.marketplace.orders;

import jakarta.validation.constraints.NotNull;

public record ShipmentUpdateRequest(@NotNull ShipmentStatus status) {
}
