package com.arquixpress.marketplace.payments;

import com.arquixpress.marketplace.catalog.ProductRepository;
import com.arquixpress.marketplace.notifications.NotificationOutboxRepository;
import com.arquixpress.marketplace.notifications.NotificationService;
import com.arquixpress.marketplace.orders.OrderEntity;
import com.arquixpress.marketplace.orders.OrderRepository;
import com.arquixpress.marketplace.orders.OrderStatus;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RF39PaymentGatewayStatusConsultationTest {

    private PaymentTransactionRepository payments;
    private OrderRepository orders;
    private ProductRepository products;
    private PaymentGatewayClient gateway;
    private PaymentReconciliationService service;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        payments = mock(PaymentTransactionRepository.class);
        orders = mock(OrderRepository.class);
        products = mock(ProductRepository.class);
        gateway = mock(PaymentGatewayClient.class);
        TransactionTemplate tx = mock(TransactionTemplate.class);
        when(tx.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        service = new PaymentReconciliationService(payments, orders, products, gateway,
                mock(NotificationOutboxRepository.class), mock(NotificationService.class), tx);
    }

    @Test
    void consultsGatewayAndSynchronizesApprovedPayment() {
        UUID orderId = UUID.randomUUID();
        OrderEntity order = new OrderEntity(orderId, UUID.randomUUID());
        PaymentTransaction payment = new PaymentTransaction(UUID.randomUUID(), orderId, "idem-1",
                new BigDecimal("50000.00"), "PSE");
        when(orders.findWithLines(orderId)).thenReturn(Optional.of(order));
        when(payments.findFirstByOrderIdOrderByCreatedAtDesc(orderId)).thenReturn(Optional.of(payment));
        when(gateway.checkStatus(eq(orderId), any())).thenReturn(PaymentGatewayResult.approved("GW-OK"));

        PaymentReconciliationResponse response = service.consultAndSync(orderId);

        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(response.orderStatus()).isEqualTo(OrderStatus.PAID);
        verify(gateway).checkStatus(eq(orderId), any());
    }

    @Test
    void rejectedGatewayStatusMarksOrderRejectedAndReleasesReservedStock() {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        OrderEntity order = new OrderEntity(orderId, UUID.randomUUID());
        order.addLine(productId, 1, new BigDecimal("50000.00"));
        PaymentTransaction payment = new PaymentTransaction(UUID.randomUUID(), orderId, "idem-2",
                new BigDecimal("50000.00"), "Tarjeta");
        when(orders.findWithLines(orderId)).thenReturn(Optional.of(order));
        when(payments.findFirstByOrderIdOrderByCreatedAtDesc(orderId)).thenReturn(Optional.of(payment));
        when(gateway.checkStatus(eq(orderId), any())).thenReturn(PaymentGatewayResult.rejected("GW-FAIL"));

        PaymentReconciliationResponse response = service.consultAndSync(orderId);

        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.REJECTED);
        assertThat(response.orderStatus()).isEqualTo(OrderStatus.PAYMENT_REJECTED);
        verify(products).releaseStock(productId, 1);
    }
}
