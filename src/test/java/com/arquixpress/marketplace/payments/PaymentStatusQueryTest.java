package com.arquixpress.marketplace.payments;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/*
 * RF-39: Consulta estado del pago
 */
class PaymentStatusQueryTest {

    @Test
    void newTransaction_shouldStartWithPendingStatus() {
        PaymentTransaction tx = new PaymentTransaction(
                UUID.randomUUID(), UUID.randomUUID(), "key-100",
                BigDecimal.valueOf(50000), "Tarjeta");

        assertEquals(PaymentStatus.PENDING, tx.status());
    }

    @Test
    void apply_shouldTransitionToApprovedWhenGatewayApproves() {
        PaymentTransaction tx = new PaymentTransaction(
                UUID.randomUUID(), UUID.randomUUID(), "key-101",
                BigDecimal.valueOf(75000), "Pago simulado");

        tx.apply(PaymentGatewayResult.approved("REF-APPR-001"));

        assertEquals(PaymentStatus.APPROVED, tx.status());
    }

    @Test
    void apply_shouldTransitionToRejectedWhenGatewayRejects() {
        PaymentTransaction tx = new PaymentTransaction(
                UUID.randomUUID(), UUID.randomUUID(), "key-102",
                BigDecimal.valueOf(30000), "Pago simulado");

        tx.apply(PaymentGatewayResult.rejected("REF-REJ-001"));

        assertEquals(PaymentStatus.REJECTED, tx.status());
    }

    @Test
    void apply_shouldOverwriteStatusOnSecondCall() {
        PaymentTransaction tx = new PaymentTransaction(
                UUID.randomUUID(), UUID.randomUUID(), "key-103",
                BigDecimal.valueOf(20000), "Pago simulado");

        tx.apply(PaymentGatewayResult.rejected("REF-FIRST"));
        tx.apply(PaymentGatewayResult.approved("REF-SECOND"));

        assertEquals(PaymentStatus.APPROVED, tx.status());
    }

    @Test
    void idempotencyKey_shouldBeStoredOnTransaction() {
        String key = "unique-key-xyz-" + UUID.randomUUID();
        PaymentTransaction tx = new PaymentTransaction(
                UUID.randomUUID(), UUID.randomUUID(), key,
                BigDecimal.valueOf(100000), "Pago simulado");

        assertEquals(key, tx.idempotencyKey());
    }

    @Test
    void transaction_shouldStoreOrderIdReference() {
        UUID orderId = UUID.randomUUID();
        PaymentTransaction tx = new PaymentTransaction(
                UUID.randomUUID(), orderId, "key-104",
                BigDecimal.valueOf(60000), "Transferencia");

        assertEquals(orderId, tx.orderId());
    }

    @Test
    void transaction_shouldStorePaymentMethod() {
        String method = "PSE";
        PaymentTransaction tx = new PaymentTransaction(
                UUID.randomUUID(), UUID.randomUUID(), "key-105",
                BigDecimal.valueOf(45000), method);

        assertEquals(method, tx.paymentMethod());
    }

    @Test
    void paymentMethod_shouldUseDefaultWhenBlankOrNull() {
        PaymentTransaction txNull = new PaymentTransaction(
                UUID.randomUUID(), UUID.randomUUID(), "key-106",
                BigDecimal.valueOf(10000), null);
        PaymentTransaction txBlank = new PaymentTransaction(
                UUID.randomUUID(), UUID.randomUUID(), "key-107",
                BigDecimal.valueOf(10000), "   ");

        assertEquals("Pago simulado", txNull.paymentMethod());
        assertEquals("Pago simulado", txBlank.paymentMethod());
    }
}
