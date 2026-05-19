package com.arquixpress.marketplace.catalog.api;

import java.math.BigDecimal;
import java.util.List;

import com.arquixpress.marketplace.catalog.ProductStatus;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SellerProductRequest(
        @NotBlank String title,
        @NotBlank String description,
        @NotBlank String category,
        String imageUrl,
        List<String> imageUrls,
        @NotNull @DecimalMin(value = "0.01") BigDecimal price,
        @Min(0) int stockAvailable,
        ProductStatus status
) {
    public ProductStatus normalizedStatus() {
        return status == null ? ProductStatus.ACTIVE : status;
    }

    public List<String> normalizedImageUrls() {
        List<String> images = imageUrls == null ? List.of() : imageUrls.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        if (!images.isEmpty()) {
            return images;
        }
        return imageUrl == null || imageUrl.isBlank() ? List.of() : List.of(imageUrl.trim());
    }
}
