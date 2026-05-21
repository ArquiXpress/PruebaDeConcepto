package com.arquixpress.marketplace.cart;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItem(UUID productId, int quantity, BigDecimal unitPrice) {
}
