package com.arquixpress.marketplace.promotions;

import com.arquixpress.marketplace.catalog.Product;
import java.math.BigDecimal;
import java.util.UUID;

public record OfferProductResponse(
        UUID id,
        String title,
        String category,
        String imageUrl,
        BigDecimal price) {
    static OfferProductResponse from(Product product) {
        return new OfferProductResponse(product.id(), product.title(), product.category(), product.imageUrl(), product.price());
    }
}
