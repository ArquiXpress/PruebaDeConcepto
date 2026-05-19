package com.arquixpress.marketplace.catalog.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.arquixpress.marketplace.catalog.Product;
import com.arquixpress.marketplace.catalog.ProductStatus;

public record SellerProductResponse(
        UUID id,
        UUID sellerId,
        String title,
        String description,
        String category,
        String imageUrl,
        List<String> imageUrls,
        BigDecimal price,
        int stockAvailable,
        ProductStatus status,
        String moderationReason,
        Instant moderationAt,
        String appealNote,
        Instant appealRequestedAt,
        String appealResolutionNote,
        Instant appealResolvedAt,
        long version,
        Instant createdAt
) {
    public static SellerProductResponse from(Product product) {
        return new SellerProductResponse(
                product.id(),
                product.sellerId(),
                product.title(),
                product.description(),
                product.category(),
                product.imageUrl(),
                product.parsedImageUrls(),
                product.price(),
                product.stockAvailable(),
                product.status(),
                product.moderationReason(),
                product.moderationAt(),
                product.appealNote(),
                product.appealRequestedAt(),
                product.appealResolutionNote(),
                product.appealResolvedAt(),
                product.version(),
                product.createdAt()
        );
    }
}
