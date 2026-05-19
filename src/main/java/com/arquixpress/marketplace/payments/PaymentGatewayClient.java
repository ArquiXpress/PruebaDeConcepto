package com.arquixpress.marketplace.payments;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentGatewayClient {
    PaymentGatewayResult charge(UUID orderId, BigDecimal amount, String idempotencyKey);

    default PaymentGatewayResult checkStatus(UUID orderId, String gatewayReference) {
        return PaymentGatewayResult.pending("Estado pendiente de confirmacion");
    }
}
