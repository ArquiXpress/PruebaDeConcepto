package com.arquixpress.marketplace.orders;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderLineResponse(UUID productId, int quantity, BigDecimal unitPrice) {
    static OrderLineResponse from(OrderLine line) {
        return new OrderLineResponse(line.productId(), line.quantity(), line.unitPrice());
    }
}
