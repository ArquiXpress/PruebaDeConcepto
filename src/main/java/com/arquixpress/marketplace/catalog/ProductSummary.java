package com.arquixpress.marketplace.catalog;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductSummary(UUID id, String title, String category, String imageUrl, BigDecimal price, int stockAvailable) {
    public static ProductSummary from(Product product) {
        return new ProductSummary(product.id(), product.title(), product.category(), product.imageUrl(), product.price(), product.stockAvailable());
    }
}
