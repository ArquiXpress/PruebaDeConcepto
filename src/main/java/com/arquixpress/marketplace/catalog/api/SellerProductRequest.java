package com.arquixpress.marketplace.catalog.api;

import java.math.BigDecimal;

import com.arquixpress.marketplace.catalog.ProductStatus;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SellerProductRequest(
        @NotBlank String title,
        @NotBlank String description,
        @NotBlank String category,
        @NotBlank String imageUrl,
        @NotNull @DecimalMin(value = "0.01") BigDecimal price,
        @Min(0) int stockAvailable,
        ProductStatus status
) {
    public ProductStatus normalizedStatus() {
        return status == null ? ProductStatus.ACTIVE : status;
    }
}