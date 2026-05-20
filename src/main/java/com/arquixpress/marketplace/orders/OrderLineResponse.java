package com.arquixpress.marketplace.orders;

import com.arquixpress.marketplace.catalog.Product;
import com.arquixpress.marketplace.identity.AppUser;
import java.math.BigDecimal;
import java.util.UUID;

public record OrderLineResponse(
        UUID productId,
        String title,
        String imageUrl,
        UUID sellerId,
        String sellerName,
        String sellerEmail,
        String sellerAddress,
        String sellerCity,
        int quantity,
        BigDecimal unitPrice) {
    static OrderLineResponse from(OrderLine line) {
        return from(line, null, null);
    }

    static OrderLineResponse from(OrderLine line, Product product, AppUser seller) {
        String title = product == null ? "Producto no disponible" : product.title();
        String imageUrl = product == null ? "" : product.imageUrl();
        return new OrderLineResponse(
                line.productId(),
                title,
                imageUrl,
                product == null ? null : product.sellerId(),
                seller == null ? "Vendedor no disponible" : seller.displayName(),
                seller == null ? "" : seller.email(),
                seller == null ? "" : seller.address(),
                seller == null ? "" : seller.city(),
                line.quantity(),
                line.unitPrice());
    }
}
