package com.arquixpress.marketplace.cart;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemResponse(UUID productId, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {
}
