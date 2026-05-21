package com.arquixpress.marketplace.orders;

import com.arquixpress.marketplace.catalog.Product;
import com.arquixpress.marketplace.catalog.ProductRepository;
import com.arquixpress.marketplace.identity.AppUserRepository;
import com.arquixpress.marketplace.logistics.LogisticsCenterRepository;
import com.arquixpress.marketplace.notifications.NotificationOutboxRepository;
import com.arquixpress.marketplace.notifications.NotificationService;
import com.arquixpress.marketplace.payments.PaymentGatewayClient;
import com.arquixpress.marketplace.payments.PaymentGatewayResult;
import com.arquixpress.marketplace.payments.PaymentStatus;
import com.arquixpress.marketplace.payments.PaymentTransaction;
import com.arquixpress.marketplace.payments.PaymentTransactionRepository;
import com.arquixpress.marketplace.promotions.CouponRedemptionRepository;
import com.arquixpress.marketplace.promotions.MarketingCouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Consumer;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/*
 * RF-36: Cancelación de pedidos antes del despacho
 */
class OrderCancellationTest {

    private ProductRepository products;
    private OrderRepository orders;
    private PaymentTransactionRepository payments;
    private PaymentGatewayClient paymentGateway;
    private NotificationOutboxRepository outbox;
    private NotificationService notifications;
    private LogisticsCenterRepository centers;
    private AppUserRepository users;
    private MarketingCouponRepository coupons;
    private CouponRedemptionRepository redemptions;
    private TransactionTemplate tx;
    private CheckoutService checkoutService;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        products = mock(ProductRepository.class);
        orders = mock(OrderRepository.class);
        payments = mock(PaymentTransactionRepository.class);
        paymentGateway = mock(PaymentGatewayClient.class);
        outbox = mock(NotificationOutboxRepository.class);
        notifications = mock(NotificationService.class);
        centers = mock(LogisticsCenterRepository.class);
        users = mock(AppUserRepository.class);
        coupons = mock(MarketingCouponRepository.class);
        redemptions = mock(CouponRedemptionRepository.class);
        tx = mock(TransactionTemplate.class);

        when(tx.execute(any(TransactionCallback.class))).thenAnswer(inv -> {
            TransactionCallback<?> callback = inv.getArgument(0);
            return callback.doInTransaction(null);
        });
        doAnswer(inv -> {
            Consumer<TransactionStatus> action = inv.getArgument(0);
            action.accept(null);
            return null;
        }).when(tx).executeWithoutResult(any());

        when(users.findById(any(UUID.class))).thenReturn(Optional.empty());
        when(users.findAllById(any())).thenReturn(List.of());

        checkoutService = new CheckoutService(products, orders, payments, paymentGateway, outbox, notifications,
                centers, users, coupons, redemptions, tx);
    }

    @Test
    void order_shouldTransitionToPaymentRejectedWhenPaymentFails() {
        OrderEntity order = new OrderEntity(UUID.randomUUID(), UUID.randomUUID());
        order.markRejected();

        assertEquals(OrderStatus.PAYMENT_REJECTED, order.status());
    }

    @Test
    void order_shouldReleaseStockOnPaymentRejection() {
        UUID buyerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        String idempotencyKey = "key-cancel-001";

        Product product = new Product(UUID.randomUUID(), "Producto", "Desc", "cat", "http://img.png",
                BigDecimal.valueOf(20000), 5);

        when(payments.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(centers.findAll()).thenReturn(List.of());
        when(products.findById(any(UUID.class))).thenReturn(Optional.of(product));
        when(products.reserveStock(any(UUID.class), eq(1))).thenReturn(1);
        when(orders.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(payments.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentGateway.charge(any(UUID.class), any(BigDecimal.class), eq(idempotencyKey)))
                .thenReturn(PaymentGatewayResult.rejected("REJECTED"));

        PaymentTransaction pendingPayment = new PaymentTransaction(UUID.randomUUID(), UUID.randomUUID(),
                idempotencyKey, BigDecimal.valueOf(20000), "Pago simulado");
        when(payments.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(pendingPayment));
        when(orders.findWithLines(any(UUID.class))).thenAnswer(inv -> {
            OrderEntity order = new OrderEntity(inv.getArgument(0), buyerId);
            order.addLine(productId, 1, BigDecimal.valueOf(20000));
            return Optional.of(order);
        });
        when(products.findAllById(any())).thenReturn(List.of(product));

        CheckoutRequest request = new CheckoutRequest(
                List.of(new CheckoutItemRequest(productId, 1)), null, "Calle 1", "Bogota");

        CheckoutResponse result = checkoutService.checkout(buyerId, request, idempotencyKey);

        assertEquals(PaymentStatus.REJECTED, result.paymentStatus());
        verify(products).releaseStock(any(UUID.class), eq(1));
    }

    @Test
    void order_shouldNotBeRetryableWhenAlreadyPaid() {
        UUID orderId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        String idempotencyKey = "key-retry-paid";

        OrderEntity paidOrder = new OrderEntity(orderId, buyerId);
        paidOrder.addLine(UUID.randomUUID(), 1, BigDecimal.valueOf(30000));
        paidOrder.markPaid();

        when(payments.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(orders.findWithLines(orderId)).thenReturn(Optional.of(paidOrder));

        CheckoutProblem ex = assertThrows(CheckoutProblem.class,
                () -> checkoutService.retryPayment(orderId, idempotencyKey));
        assertEquals("ORDER_NOT_RETRYABLE", ex.code());
    }

    @Test
    void order_shouldAllowRetryWhenPaymentWasRejected() {
        UUID orderId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        String idempotencyKey = "key-retry-ok";

        Product product = new Product(UUID.randomUUID(), "Producto", "Desc", "cat", "http://img.png",
                BigDecimal.valueOf(40000), 10);

        OrderEntity rejectedOrder = new OrderEntity(orderId, buyerId);
        rejectedOrder.addLine(productId, 1, BigDecimal.valueOf(40000));
        rejectedOrder.markRejected();

        PaymentTransaction prevPayment = new PaymentTransaction(UUID.randomUUID(), orderId,
                "key-old", BigDecimal.valueOf(40000), "Pago simulado");

        when(payments.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(orders.findWithLines(orderId)).thenReturn(Optional.of(rejectedOrder));
        when(products.reserveStock(any(UUID.class), eq(1))).thenReturn(1);
        when(payments.findFirstByOrderIdOrderByCreatedAtDesc(orderId)).thenReturn(Optional.of(prevPayment));
        when(payments.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentGateway.charge(any(UUID.class), any(BigDecimal.class), eq(idempotencyKey)))
                .thenReturn(PaymentGatewayResult.approved("REF-RETRY"));

        PaymentTransaction retryPayment = new PaymentTransaction(UUID.randomUUID(), orderId,
                idempotencyKey, BigDecimal.valueOf(40000), "Pago simulado");
        when(payments.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(retryPayment));
        when(orders.findWithLines(orderId)).thenAnswer(inv -> {
            OrderEntity order = new OrderEntity(orderId, buyerId);
            order.addLine(productId, 1, BigDecimal.valueOf(40000));
            order.markRejected();
            return Optional.of(order);
        });
        when(products.findAllById(any())).thenReturn(List.of(product));
        when(users.findAllById(any())).thenReturn(List.of());

        CheckoutResponse result = checkoutService.retryPayment(orderId, idempotencyKey);

        assertNotNull(result);
        assertEquals(PaymentStatus.APPROVED, result.paymentStatus());
    }

    @Test
    void order_shouldNotUpdateShipmentWhenNotPaid() {
        OrderEntity order = new OrderEntity(UUID.randomUUID(), UUID.randomUUID());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> order.updateShipment(ShipmentStatus.IN_ROUTE));
        assertEquals("Solo pedidos pagados pueden cambiar estado logistico", ex.getMessage());
    }

    @Test
    void order_shouldNotificationBuyerOnRejection() {
        UUID buyerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        String idempotencyKey = "key-notify-reject";

        Product product = new Product(UUID.randomUUID(), "Prod", "Desc", "cat", "http://img.png",
                BigDecimal.valueOf(10000), 2);

        when(payments.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(centers.findAll()).thenReturn(List.of());
        when(products.findById(any(UUID.class))).thenReturn(Optional.of(product));
        when(products.reserveStock(any(UUID.class), eq(1))).thenReturn(1);
        when(orders.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(payments.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentGateway.charge(any(UUID.class), any(BigDecimal.class), eq(idempotencyKey)))
                .thenReturn(PaymentGatewayResult.rejected("FAIL"));

        PaymentTransaction pt = new PaymentTransaction(UUID.randomUUID(), UUID.randomUUID(),
                idempotencyKey, BigDecimal.valueOf(10000), "Pago simulado");
        when(payments.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(pt));
        when(orders.findWithLines(any(UUID.class))).thenAnswer(inv -> {
            OrderEntity order = new OrderEntity(inv.getArgument(0), buyerId);
            order.addLine(productId, 1, BigDecimal.valueOf(10000));
            return Optional.of(order);
        });
        when(products.findAllById(any())).thenReturn(List.of(product));

        checkoutService.checkout(buyerId,
                new CheckoutRequest(List.of(new CheckoutItemRequest(productId, 1)), null, "Calle 1", "Bogota"),
                idempotencyKey);

        verify(notifications).notify(eq(buyerId), eq("PAYMENT_REJECTED"), anyString(), anyString(), anyString());
    }
}
