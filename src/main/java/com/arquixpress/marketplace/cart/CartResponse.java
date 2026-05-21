package com.arquixpress.marketplace.cart;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(List<CartItemResponse> items, BigDecimal total) {
}
