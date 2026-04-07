package com.arquixpress.marketplace.payments;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class MockPaymentGatewayClient implements PaymentGatewayClient {
    private final double approvalRate;
    private final double timeoutRate;
    private final int minLatencyMs;
    private final int maxLatencyMs;

    MockPaymentGatewayClient(
            @Value("${app.payment.approval-rate}") double approvalRate,
            @Value("${app.payment.timeout-rate}") double timeoutRate,
            @Value("${app.payment.min-latency-ms}") int minLatencyMs,
            @Value("${app.payment.max-latency-ms}") int maxLatencyMs) {
        this.approvalRate = approvalRate;
        this.timeoutRate = timeoutRate;
        this.minLatencyMs = minLatencyMs;
        this.maxLatencyMs = maxLatencyMs;
    }

    @Override
    public PaymentGatewayResult charge(UUID orderId, BigDecimal amount, String idempotencyKey) {
        simulateLatency();
        double sample = ThreadLocalRandom.current().nextDouble();
        if (sample < timeoutRate) {
            return PaymentGatewayResult.pending("Pasarela lenta o sin respuesta; pago queda pendiente para reintento");
        }
        String reference = "mock-gw-" + UUID.nameUUIDFromBytes((orderId + idempotencyKey).getBytes());
        if (sample <= timeoutRate + approvalRate) {
            return PaymentGatewayResult.approved(reference);
        }
        return PaymentGatewayResult.rejected(reference);
    }

    private void simulateLatency() {
        int latency = ThreadLocalRandom.current().nextInt(minLatencyMs, Math.max(minLatencyMs + 1, maxLatencyMs + 1));
        try {
            Thread.sleep(latency);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
