package com.arquixpress.marketplace.orders;

import com.arquixpress.marketplace.payments.PaymentStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record CheckoutResponse(UUID orderId, OrderStatus orderStatus, PaymentStatus paymentStatus, BigDecimal total, String message) {
    static CheckoutResponse from(OrderEntity order, PaymentStatus paymentStatus, String message) {
        return new CheckoutResponse(order.id(), order.status(), paymentStatus, order.total(), message);
    }
}
