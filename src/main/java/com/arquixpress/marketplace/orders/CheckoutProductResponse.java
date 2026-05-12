package com.arquixpress.marketplace.orders;

import com.arquixpress.marketplace.catalog.Product;
import java.math.BigDecimal;
import java.util.UUID;

public record CheckoutProductResponse(
        UUID productId,
        String title,
        String imageUrl,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal) {

    static CheckoutProductResponse from(OrderLine line, Product product) {
        String title = product != null ? product.title() : "Producto no disponible";
        String imageUrl = product != null ? product.imageUrl() : "";
        BigDecimal subtotal = line.unitPrice().multiply(BigDecimal.valueOf(line.quantity()));
        return new CheckoutProductResponse(line.productId(), title, imageUrl, line.quantity(), line.unitPrice(), subtotal);
    }
}
