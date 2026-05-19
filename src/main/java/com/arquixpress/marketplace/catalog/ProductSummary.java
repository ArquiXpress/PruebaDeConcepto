package com.arquixpress.marketplace.catalog;

import java.math.BigDecimal;
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
        int stockAvailable) {

    public static ProductSummary from(Product product) {
        return new ProductSummary(product.id(), product.sellerId(), product.title(), product.description(),
                product.category(), product.imageUrl(), Product.parseImages(product.imageUrls(), product.imageUrl()),
                product.price(), product.stockAvailable());
    }

    public static List<String> parseImages(String imageUrls, String fallbackImageUrl) {
        return Product.parseImages(imageUrls, fallbackImageUrl);
    }
}
