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
        BigDecimal shippingCost,
        BigDecimal discountTotal,
        String couponCode,
        String shippingAddress,
        String shippingCity,
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
                order.shippingCost(),
                order.discountTotal(),
                order.couponCode(),
                order.shippingAddress(),
                order.shippingCity(),
                items,
                message);
    }
}
