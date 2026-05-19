package com.arquixpress.marketplace.orders;

import com.arquixpress.marketplace.catalog.ProductRepository;
import com.arquixpress.marketplace.logistics.LogisticsCenterRepository;
import com.arquixpress.marketplace.notifications.NotificationOutboxRepository;
import com.arquixpress.marketplace.notifications.NotificationService;
import com.arquixpress.marketplace.payments.PaymentGatewayClient;
import com.arquixpress.marketplace.payments.PaymentTransactionRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RF36CancelOrderBeforeDispatchTest {

    private ProductRepository products;
    private OrderRepository orders;
    private NotificationOutboxRepository outbox;
    private NotificationService notifications;
    private CheckoutService service;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        products = mock(ProductRepository.class);
        orders = mock(OrderRepository.class);
        outbox = mock(NotificationOutboxRepository.class);
        notifications = mock(NotificationService.class);
        TransactionTemplate tx = mock(TransactionTemplate.class);
        when(tx.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            callback.doInTransaction(null);
            return null;
        }).when(tx).executeWithoutResult(any());

        service = new CheckoutService(products, orders, mock(PaymentTransactionRepository.class),
                mock(PaymentGatewayClient.class), outbox, notifications, mock(LogisticsCenterRepository.class), tx);
    }

    @Test
    void buyerCanCancelPaidOrderBeforeShipmentAndStockIsReleased() {
        UUID buyerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        OrderEntity order = new OrderEntity(orderId, buyerId);
        order.addLine(productId, 2, new BigDecimal("12000.00"));
        order.markPaid();
        when(orders.findWithLines(orderId)).thenReturn(Optional.of(order));

        OrderResponse response = service.cancelOrder(buyerId, orderId);

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        verify(products).releaseStock(productId, 2);
        verify(outbox).save(any());
        verify(notifications).notify(eq(buyerId), eq("ORDER_CANCELLED"), any(), any(), any());
    }

    @Test
    void orderCannotBeCancelledAfterDispatch() {
        UUID buyerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        OrderEntity order = new OrderEntity(orderId, buyerId);
        order.markPaid();
        order.updateShipment(ShipmentStatus.IN_ROUTE);
        when(orders.findWithLines(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.cancelOrder(buyerId, orderId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("antes del despacho");
    }
}
