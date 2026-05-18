package com.arquixpress.marketplace.payments;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

class PaymentTransactionTest {

    @Test
    void newTransaction_shouldStartWithPendingStatus() {
        PaymentTransaction tx = new PaymentTransaction(
                UUID.randomUUID(), UUID.randomUUID(), "key-1", BigDecimal.valueOf(50000), "Tarjeta credito"
        );

        assertEquals(PaymentStatus.PENDING, tx.status());
        assertEquals("key-1", tx.idempotencyKey());
        assertEquals("Tarjeta credito", tx.paymentMethod());
        assertNotNull(tx.transactionId());
    }

    @Test
    void apply_shouldTransitionToApprovedWhenResultApproved() {
        PaymentTransaction tx = new PaymentTransaction(
                UUID.randomUUID(), UUID.randomUUID(), "key-2", BigDecimal.valueOf(30000), "PSE"
        );

        tx.apply(PaymentGatewayResult.approved("EXT-REF-001"));

        assertEquals(PaymentStatus.APPROVED, tx.status());
    }

    @Test
    void apply_shouldTransitionToRejectedWhenResultRejected() {
        PaymentTransaction tx = new PaymentTransaction(
                UUID.randomUUID(), UUID.randomUUID(), "key-3", BigDecimal.valueOf(20000), "Nequi"
        );

        tx.apply(PaymentGatewayResult.rejected("EXT-REF-002"));

        assertEquals(PaymentStatus.REJECTED, tx.status());
    }

    @Test
    void apply_shouldOverwriteStatusOnSecondCall() {
        PaymentTransaction tx = new PaymentTransaction(
                UUID.randomUUID(), UUID.randomUUID(), "key-4", BigDecimal.valueOf(10000), "Pago simulado"
        );

        tx.apply(PaymentGatewayResult.rejected("REF-FIRST"));
        assertEquals(PaymentStatus.REJECTED, tx.status());

        tx.apply(PaymentGatewayResult.approved("REF-SECOND"));
        assertEquals(PaymentStatus.APPROVED, tx.status());
    }

    @Test
    void paymentMethod_shouldReturnDefaultWhenBlank() {
        PaymentTransaction tx = new PaymentTransaction(
                UUID.randomUUID(), UUID.randomUUID(), "key-5", BigDecimal.valueOf(5000), "  "
        );

        assertEquals("Pago simulado", tx.paymentMethod());
    }

    @Test
    void paymentMethod_shouldReturnDefaultWhenNull() {
        PaymentTransaction tx = new PaymentTransaction(
                UUID.randomUUID(), UUID.randomUUID(), "key-6", BigDecimal.valueOf(5000), null
        );

        assertEquals("Pago simulado", tx.paymentMethod());
    }

    @Test
    void idempotencyKey_shouldBeUniquePerTransaction() {
        PaymentTransaction tx1 = new PaymentTransaction(
                UUID.randomUUID(), UUID.randomUUID(), "key-unique-1", BigDecimal.valueOf(1000), "Pago simulado"
        );
        PaymentTransaction tx2 = new PaymentTransaction(
                UUID.randomUUID(), UUID.randomUUID(), "key-unique-2", BigDecimal.valueOf(2000), "Pago simulado"
        );

        assertEquals("key-unique-1", tx1.idempotencyKey());
        assertEquals("key-unique-2", tx2.idempotencyKey());
    }
}
