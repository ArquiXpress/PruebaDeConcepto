package com.arquixpress.marketplace.promotions;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record PromotionRequest(@NotBlank String name, @NotNull Instant startsAt, @NotNull Instant endsAt) {
}
