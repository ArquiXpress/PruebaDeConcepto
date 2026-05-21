package com.arquixpress.marketplace.orders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.arquixpress.marketplace.catalog.Product;
import com.arquixpress.marketplace.catalog.ProductRepository;
import com.arquixpress.marketplace.catalog.ProductStatus;
import com.arquixpress.marketplace.logistics.LogisticsCenterRepository;
import com.arquixpress.marketplace.notifications.NotificationOutboxRepository;
import com.arquixpress.marketplace.notifications.NotificationService;
import com.arquixpress.marketplace.payments.PaymentGatewayClient;
import com.arquixpress.marketplace.payments.PaymentGatewayResult;
import com.arquixpress.marketplace.payments.PaymentStatus;
import com.arquixpress.marketplace.payments.PaymentTransaction;
import com.arquixpress.marketplace.payments.PaymentTransactionRepository;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

class CheckoutServiceTest {

    private ProductRepository products;
    private OrderRepository orders;
    private PaymentTransactionRepository payments;
    private PaymentGatewayClient paymentGateway;
    private NotificationOutboxRepository outbox;
    private NotificationService notifications;
    private LogisticsCenterRepository centers;
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
        tx = mock(TransactionTemplate.class);

        when(tx.execute(any())).thenAnswer(inv -> {
            Object callback = inv.getArgument(0);
            if (callback instanceof TransactionCallback) {
                return ((TransactionCallback<?>) callback).doInTransaction(null);
            }
            return null;
        });
        doAnswer(inv -> {
            Object callback = inv.getArgument(0);
            if (callback instanceof java.util.function.Consumer) {
                ((java.util.function.Consumer<?>) callback).accept(null);
            } else if (callback instanceof TransactionCallback) {
                ((TransactionCallback<?>) callback).doInTransaction(null);
            }
            return null;
        }).when(tx).executeWithoutResult(any());

        checkoutService = new CheckoutService(products, orders, payments, paymentGateway, outbox, notifications, centers, tx);
    }

    @Test
    void checkout_shouldCreateOrderAndApprovePayment() {
        UUID buyerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        String idempotencyKey = "key-001";

        Product product = new Product(UUID.randomUUID(), "Producto Test", "Descripcion", "electronica", "http://img.png", BigDecimal.valueOf(50000), 10);

        when(payments.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(centers.findAll()).thenReturn(List.of());
        when(products.findByIdAndStatus(any(UUID.class), eq(ProductStatus.ACTIVE))).thenReturn(Optional.of(product));
        when(products.reserveStock(any(UUID.class), eq(2))).thenReturn(1);
        when(orders.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(payments.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentGateway.charge(any(UUID.class), any(BigDecimal.class), eq(idempotencyKey)))
                .thenReturn(PaymentGatewayResult.approved("REF-123"));
        when(payments.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new PaymentTransaction(UUID.randomUUID(), UUID.randomUUID(), idempotencyKey, BigDecimal.valueOf(100000), "Pago simulado")));
        when(orders.findWithLines(any(UUID.class))).thenAnswer(inv -> {
            OrderEntity order = new OrderEntity(inv.getArgument(0), buyerId);
            order.addLine(productId, 2, BigDecimal.valueOf(50000));
            order.markPaid();
            return Optional.of(order);
        });
        when(products.findAllById(any())).thenReturn(List.of(product));

        CheckoutRequest request = new CheckoutRequest(
                List.of(new CheckoutItemRequest(productId, 2)),
                null
        );

        CheckoutResponse result = checkoutService.checkout(buyerId, request, idempotencyKey);

        assertNotNull(result);
        assertEquals(OrderStatus.PAID, result.orderStatus());
        verify(products).reserveStock(any(UUID.class), eq(2));
        verify(paymentGateway).charge(any(UUID.class), any(BigDecimal.class), eq(idempotencyKey));
    }

    @Test
    void checkout_shouldRollbackStockWhenPaymentRejected() {
        UUID buyerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        String idempotencyKey = "key-002";

        Product product = new Product(UUID.randomUUID(), "Producto Test", "Descripcion", "electronica", "http://img.png", BigDecimal.valueOf(30000), 5);

        when(payments.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(centers.findAll()).thenReturn(List.of());
        when(products.findByIdAndStatus(any(UUID.class), eq(ProductStatus.ACTIVE))).thenReturn(Optional.of(product));
        when(products.reserveStock(any(UUID.class), eq(1))).thenReturn(1);
        when(orders.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(payments.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentGateway.charge(any(UUID.class), any(BigDecimal.class), eq(idempotencyKey)))
                .thenReturn(PaymentGatewayResult.rejected("REF-FAIL"));

        PaymentTransaction pendingPayment = new PaymentTransaction(UUID.randomUUID(), UUID.randomUUID(), idempotencyKey, BigDecimal.valueOf(30000), "Pago simulado");
        when(payments.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(pendingPayment));
        when(orders.findWithLines(any(UUID.class))).thenAnswer(inv -> {
            OrderEntity order = new OrderEntity(inv.getArgument(0), buyerId);
            order.addLine(productId, 1, BigDecimal.valueOf(30000));
            return Optional.of(order);
        });
        when(products.findAllById(any())).thenReturn(List.of(product));

        CheckoutRequest request = new CheckoutRequest(
                List.of(new CheckoutItemRequest(productId, 1)),
                null
        );

        CheckoutResponse result = checkoutService.checkout(buyerId, request, idempotencyKey);

        assertNotNull(result);
        assertEquals(PaymentStatus.REJECTED, result.paymentStatus());
        verify(products).releaseStock(any(UUID.class), eq(1));
    }

    @Test
    void checkout_shouldReturnCachedResponseWhenIdempotencyKeyExists() {
        UUID buyerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        String idempotencyKey = "key-duplicate";

        PaymentTransaction existingPayment = new PaymentTransaction(UUID.randomUUID(), orderId, idempotencyKey, BigDecimal.valueOf(50000), "Pago simulado");
        OrderEntity existingOrder = new OrderEntity(orderId, buyerId);
        existingOrder.addLine(UUID.randomUUID(), 1, BigDecimal.valueOf(50000));
        existingOrder.markPaid();

        when(payments.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existingPayment));
        when(orders.findWithLines(orderId)).thenReturn(Optional.of(existingOrder));
        when(products.findAllById(any())).thenReturn(List.of());

        CheckoutRequest request = new CheckoutRequest(
                List.of(new CheckoutItemRequest(UUID.randomUUID(), 1)),
                null
        );

        CheckoutResponse result = checkoutService.checkout(buyerId, request, idempotencyKey);

        assertNotNull(result);
        assertEquals(orderId, result.orderId());
        assertEquals("Resultado idempotente; no se crea otro cobro ni otra orden", result.message());
        verify(paymentGateway, never()).charge(any(), any(), any());
    }

    @Test
    void checkout_shouldThrowWhenIdempotencyKeyIsBlank() {
        UUID buyerId = UUID.randomUUID();
        CheckoutRequest request = new CheckoutRequest(List.of(new CheckoutItemRequest(UUID.randomUUID(), 1)), null);

        CheckoutProblem ex = assertThrows(CheckoutProblem.class, () -> checkoutService.checkout(buyerId, request, "   "));
        assertEquals("IDEMPOTENCY_KEY_REQUIRED", ex.code());
    }

    @Test
    void checkout_shouldThrowWhenProductNotFound() {
        UUID buyerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        String idempotencyKey = "key-notfound";

        when(payments.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(centers.findAll()).thenReturn(List.of());
        when(products.findByIdAndStatus(productId, ProductStatus.ACTIVE)).thenReturn(Optional.empty());

        CheckoutRequest request = new CheckoutRequest(List.of(new CheckoutItemRequest(productId, 1)), null);

        CheckoutProblem ex = assertThrows(CheckoutProblem.class, () -> checkoutService.checkout(buyerId, request, idempotencyKey));
        assertEquals("PRODUCT_NOT_FOUND", ex.code());
    }

    @Test
    void checkout_shouldThrowWhenInsufficientStock() {
        UUID buyerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        String idempotencyKey = "key-nostock";

        Product product = new Product(UUID.randomUUID(), "Producto", "Desc", "cat", "http://img.png", BigDecimal.valueOf(10000), 1);

        when(payments.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(centers.findAll()).thenReturn(List.of());
        when(products.findByIdAndStatus(any(UUID.class), eq(ProductStatus.ACTIVE))).thenReturn(Optional.of(product));
        when(products.reserveStock(any(UUID.class), eq(99))).thenReturn(0);

        CheckoutRequest request = new CheckoutRequest(List.of(new CheckoutItemRequest(productId, 99)), null);

        CheckoutProblem ex = assertThrows(CheckoutProblem.class, () -> checkoutService.checkout(buyerId, request, idempotencyKey));
        assertEquals("INSUFFICIENT_STOCK", ex.code());
    }

    @Test
    void retryPayment_shouldRetryRejectedOrderSuccessfully() {
        UUID buyerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        String idempotencyKey = "key-retry-001";

        Product product = new Product(UUID.randomUUID(), "Producto", "Desc", "cat", "http://img.png", BigDecimal.valueOf(50000), 10);

        OrderEntity rejectedOrder = new OrderEntity(orderId, buyerId);
        rejectedOrder.addLine(productId, 1, BigDecimal.valueOf(50000));
        rejectedOrder.markRejected();

        PaymentTransaction firstPayment = new PaymentTransaction(UUID.randomUUID(), orderId, "first-key", BigDecimal.valueOf(50000), "Pago simulado");

        when(payments.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(orders.findWithLines(orderId)).thenReturn(Optional.of(rejectedOrder));
        when(products.reserveStock(productId, 1)).thenReturn(1);
        when(payments.findFirstByOrderIdOrderByCreatedAtDesc(orderId)).thenReturn(Optional.of(firstPayment));
        when(payments.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentGateway.charge(orderId, BigDecimal.valueOf(50000), idempotencyKey))
                .thenReturn(PaymentGatewayResult.approved("REF-RETRY"));
        PaymentTransaction retryPayment = new PaymentTransaction(UUID.randomUUID(), orderId, idempotencyKey, BigDecimal.valueOf(50000), "Pago simulado");
        when(payments.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(retryPayment));
        when(products.findAllById(any())).thenReturn(List.of(product));

        CheckoutResponse result = checkoutService.retryPayment(orderId, idempotencyKey);

        assertNotNull(result);
        assertEquals(OrderStatus.PAID, result.orderStatus());
        verify(products).reserveStock(any(UUID.class), eq(1));
    }

    @Test
    void retryPayment_shouldThrowWhenOrderNotRetryable() {
        UUID orderId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        String idempotencyKey = "key-not-retryable";

        OrderEntity paidOrder = new OrderEntity(orderId, buyerId);
        paidOrder.markPaid();

        when(payments.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(orders.findWithLines(orderId)).thenReturn(Optional.of(paidOrder));

        CheckoutProblem ex = assertThrows(CheckoutProblem.class, () -> checkoutService.retryPayment(orderId, idempotencyKey));
        assertEquals("ORDER_NOT_RETRYABLE", ex.code());
    }

    @Test
    void retryPayment_shouldReturnCachedResponseWhenIdempotencyKeyExists() {
        UUID orderId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        String idempotencyKey = "key-cached-retry";

        PaymentTransaction cachedPayment = new PaymentTransaction(UUID.randomUUID(), orderId, idempotencyKey, BigDecimal.valueOf(50000), "Pago simulado");
        OrderEntity order = new OrderEntity(orderId, buyerId);
        order.addLine(UUID.randomUUID(), 1, BigDecimal.valueOf(50000));
        order.markPaid();

        when(payments.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(cachedPayment));
        when(orders.findWithLines(orderId)).thenReturn(Optional.of(order));
        when(products.findAllById(any())).thenReturn(List.of());

        CheckoutResponse result = checkoutService.retryPayment(orderId, idempotencyKey);

        assertNotNull(result);
        assertEquals("Resultado idempotente de reintento", result.message());
    }

    @Test
    void getOrdersForBuyer_shouldReturnAllOrdersForBuyer() {
        UUID buyerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        OrderEntity order1 = new OrderEntity(UUID.randomUUID(), buyerId);
        order1.addLine(productId, 2, BigDecimal.valueOf(30000));
        order1.markPaid();

        OrderEntity order2 = new OrderEntity(UUID.randomUUID(), buyerId);
        order2.addLine(productId, 1, BigDecimal.valueOf(30000));
        order2.markPaid();

        when(orders.findByBuyerWithLines(buyerId)).thenReturn(List.of(order1, order2));
        when(products.findAllById(any())).thenReturn(List.of(
                new Product(UUID.randomUUID(), "Producto", "Desc", "cat", "http://img.png", BigDecimal.valueOf(30000), 10)
        ));

        List<OrderResponse> result = checkoutService.getOrdersForBuyer(buyerId);

        assertEquals(2, result.size());
    }

    @Test
    void getOrdersForBuyer_shouldReturnEmptyListWhenNoPurchases() {
        UUID buyerId = UUID.randomUUID();

        when(orders.findByBuyerWithLines(buyerId)).thenReturn(List.of());

        List<OrderResponse> result = checkoutService.getOrdersForBuyer(buyerId);

        assertEquals(0, result.size());
    }
}
