package com.arquixpress.marketplace.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.List;

public record ProductCreateRequest(
        @NotBlank String title,
        @NotBlank String description,
        @NotBlank String category,
        List<String> imageUrls,
        @NotNull @PositiveOrZero BigDecimal price,
        @PositiveOrZero int stockAvailable) {
}
