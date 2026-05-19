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
import com.arquixpress.marketplace.identity.AppUserRepository;
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
    private AppUserRepository users;
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
        tx = mock(TransactionTemplate.class);

        when(tx.execute(any(TransactionCallback.class))).thenAnswer(inv -> {
            TransactionCallback<?> callback = inv.getArgument(0);
            return callback.doInTransaction(null);
        });
        doAnswer(inv -> {
            TransactionCallback<?> callback = inv.getArgument(0);
            callback.doInTransaction(null);
            return null;
        }).when(tx).executeWithoutResult(any());

        when(users.findById(any(UUID.class))).thenReturn(Optional.empty());
        when(users.findAllById(any())).thenReturn(List.of());

        checkoutService = new CheckoutService(products, orders, payments, paymentGateway, outbox, notifications, centers, users, tx);
    }

    @Test
    void checkout_shouldCreateOrderAndApprovePayment() {
        UUID buyerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        String idempotencyKey = "key-001";

        Product product = new Product(UUID.randomUUID(), "Producto Test", "Descripcion", "electronica", "http://img.png", BigDecimal.valueOf(50000), 10);

        when(payments.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(centers.findAll()).thenReturn(List.of());
        when(products.findById(any(UUID.class))).thenReturn(Optional.of(product));
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

        CheckoutRequest request = request(productId, 2);

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
        when(products.findById(any(UUID.class))).thenReturn(Optional.of(product));
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

        CheckoutRequest request = request(productId, 1);

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

        CheckoutRequest request = request(UUID.randomUUID(), 1);

        CheckoutResponse result = checkoutService.checkout(buyerId, request, idempotencyKey);

        assertNotNull(result);
        assertEquals(orderId, result.orderId());
        assertEquals("Resultado idempotente; no se crea otro cobro ni otra orden", result.message());
        verify(paymentGateway, never()).charge(any(), any(), any());
    }

    @Test
    void checkout_shouldThrowWhenIdempotencyKeyIsBlank() {
        UUID buyerId = UUID.randomUUID();
        CheckoutRequest request = request(UUID.randomUUID(), 1);

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
        when(products.findById(productId)).thenReturn(Optional.empty());

        CheckoutRequest request = request(productId, 1);

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
        when(products.findById(any(UUID.class))).thenReturn(Optional.of(product));
        when(products.reserveStock(any(UUID.class), eq(99))).thenReturn(0);

        CheckoutRequest request = request(productId, 99);

        CheckoutProblem ex = assertThrows(CheckoutProblem.class, () -> checkoutService.checkout(buyerId, request, idempotencyKey));
        assertEquals("INSUFFICIENT_STOCK", ex.code());
    }

    private CheckoutRequest request(UUID productId, int quantity) {
        return new CheckoutRequest(
                List.of(new CheckoutItemRequest(productId, quantity)),
                null,
                "Calle 123 #45-67",
                "Bogota"
        );
    }
}
