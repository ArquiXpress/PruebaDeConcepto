package com.arquixpress.marketplace.promotions;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CouponCreateRequest(
        @NotBlank String code,
        @NotBlank String title,
        @NotBlank String description,
        @Min(1) @Max(90) int discountPercent,
        @NotNull CouponTargetType targetType,
        String targetValue) {
}
