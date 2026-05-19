package com.arquixpress.marketplace.payments;

import com.arquixpress.marketplace.orders.OrderStatus;
import java.util.UUID;

public record PaymentReconciliationResponse(
        UUID orderId,
        String transactionId,
        PaymentStatus paymentStatus,
        OrderStatus orderStatus,
        String message) {
}
