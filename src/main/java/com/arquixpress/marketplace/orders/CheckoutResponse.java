package com.arquixpress.marketplace.orders;

import com.arquixpress.marketplace.payments.PaymentStatus;
import com.arquixpress.marketplace.payments.PaymentTransaction;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CheckoutResponse(
        UUID orderId,
        OrderStatus orderStatus,
        ShipmentStatus shipmentStatus,
        PaymentStatus paymentStatus,
        String paymentMethod,
        String transactionId,
        BigDecimal total,
        UUID deliveryAddressId,
        String deliveryAddressSnapshot,
        List<CheckoutProductResponse> items,
        String message) {

    static CheckoutResponse from(OrderEntity order, PaymentTransaction payment, List<CheckoutProductResponse> items, String message) {
        return new CheckoutResponse(
                order.id(),
                order.status(),
                order.shipmentStatus(),
                payment.status(),
                payment.paymentMethod(),
                payment.transactionId(),
                order.total(),
                order.deliveryAddressId(),
                order.deliveryAddressSnapshot(),
                items,
                message);
    }
}
