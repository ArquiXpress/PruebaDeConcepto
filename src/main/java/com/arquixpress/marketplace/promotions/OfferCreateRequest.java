package com.arquixpress.marketplace.promotions;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OfferCreateRequest(
        @NotNull UUID sellerId,
        @NotBlank String title,
        @NotBlank String message,
        @Min(1) @Max(90) int discountPercent,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt,
        @NotEmpty List<UUID> productIds) {
}
