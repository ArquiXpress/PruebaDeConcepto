package com.arquixpress.marketplace.catalog;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductSummary(
        UUID id,
        UUID sellerId,
        String title,
        String description,
        String category,
        String imageUrl,
        List<String> imageUrls,
        BigDecimal price,
        Integer discountPercent,
        BigDecimal salePrice,
        Instant offerEndsAt,
        int stockAvailable) {

    public static ProductSummary from(Product product) {
        return new ProductSummary(product.id(), product.sellerId(), product.title(), product.description(),
                product.category(), product.imageUrl(), Product.parseImages(product.imageUrls(), product.imageUrl()),
                product.price(), null, null, null, product.stockAvailable());
    }

    public static ProductSummary discounted(Product product, int discountPercent, Instant offerEndsAt) {
        BigDecimal multiplier = BigDecimal.valueOf(100 - discountPercent)
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return new ProductSummary(product.id(), product.sellerId(), product.title(), product.description(),
                product.category(), product.imageUrl(), Product.parseImages(product.imageUrls(), product.imageUrl()),
                product.price(), discountPercent, product.price().multiply(multiplier).setScale(2, RoundingMode.HALF_UP),
                offerEndsAt, product.stockAvailable());
    }

    public static List<String> parseImages(String imageUrls, String fallbackImageUrl) {
        return Product.parseImages(imageUrls, fallbackImageUrl);
    }
}
