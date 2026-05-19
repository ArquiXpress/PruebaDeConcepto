package com.arquixpress.marketplace.catalog;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.arquixpress.marketplace.identity.AppUserRepository;
import com.arquixpress.marketplace.logistics.LogisticsCenterRepository;
import com.arquixpress.marketplace.notifications.NotificationOutboxRepository;
import com.arquixpress.marketplace.notifications.NotificationService;
import com.arquixpress.marketplace.orders.CheckoutItemRequest;
import com.arquixpress.marketplace.orders.CheckoutProblem;
import com.arquixpress.marketplace.orders.CheckoutRequest;
import com.arquixpress.marketplace.orders.CheckoutService;
import com.arquixpress.marketplace.orders.OrderRepository;
import com.arquixpress.marketplace.payments.PaymentGatewayClient;
import com.arquixpress.marketplace.payments.PaymentGatewayResult;
import com.arquixpress.marketplace.payments.PaymentTransaction;
import com.arquixpress.marketplace.payments.PaymentTransactionRepository;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

class ProductStockTest {

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
    void reserveStock_shouldSucceedWhenStockAvailable() {
        UUID productId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        String key = "stock-ok";

        Product product = new Product(UUID.randomUUID(), "Producto", "Desc", "cat", "http://img.png", BigDecimal.valueOf(10000), 10);

        when(payments.findByIdempotencyKey(key)).thenReturn(Optional.empty());
        when(centers.findAll()).thenReturn(List.of());
        when(products.findById(any(UUID.class))).thenReturn(Optional.of(product));
        when(products.reserveStock(any(UUID.class), eq(5))).thenReturn(1);
        when(orders.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(payments.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentGateway.charge(any(), any(), eq(key))).thenReturn(PaymentGatewayResult.approved("REF"));
        when(payments.findByIdempotencyKey(key))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new PaymentTransaction(UUID.randomUUID(), UUID.randomUUID(), key, BigDecimal.valueOf(50000), "Pago simulado")));
        when(orders.findWithLines(any())).thenAnswer(inv -> {
            var order = new com.arquixpress.marketplace.orders.OrderEntity(inv.getArgument(0), buyerId);
            order.addLine(productId, 5, BigDecimal.valueOf(10000));
            order.markPaid();
            return Optional.of(order);
        });
        when(products.findAllById(any())).thenReturn(List.of(product));

        CheckoutRequest request = request(productId, 5);
        checkoutService.checkout(buyerId, request, key);

        verify(products).reserveStock(any(UUID.class), eq(5));
    }

    @Test
    void reserveStock_shouldFailWhenInsufficientStock() {
        UUID productId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        String key = "stock-fail";

        Product product = new Product(UUID.randomUUID(), "Producto", "Desc", "cat", "http://img.png", BigDecimal.valueOf(10000), 2);

        when(payments.findByIdempotencyKey(key)).thenReturn(Optional.empty());
        when(centers.findAll()).thenReturn(List.of());
        when(products.findById(any(UUID.class))).thenReturn(Optional.of(product));
        when(products.reserveStock(any(UUID.class), eq(100))).thenReturn(0);

        CheckoutRequest request = request(productId, 100);

        CheckoutProblem ex = assertThrows(CheckoutProblem.class, () -> checkoutService.checkout(buyerId, request, key));
        assertEquals("INSUFFICIENT_STOCK", ex.code());
    }

    @Test
    void releaseStock_shouldBeCalledOnPaymentRejection() {
        UUID productId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        String key = "stock-release";

        Product product = new Product(UUID.randomUUID(), "Producto", "Desc", "cat", "http://img.png", BigDecimal.valueOf(20000), 10);

        when(payments.findByIdempotencyKey(key)).thenReturn(Optional.empty());
        when(centers.findAll()).thenReturn(List.of());
        when(products.findById(any(UUID.class))).thenReturn(Optional.of(product));
        when(products.reserveStock(any(UUID.class), eq(3))).thenReturn(1);
        when(orders.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(payments.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentGateway.charge(any(), any(), eq(key))).thenReturn(PaymentGatewayResult.rejected("REF-FAIL"));
        when(payments.findByIdempotencyKey(key))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new PaymentTransaction(UUID.randomUUID(), UUID.randomUUID(), key, BigDecimal.valueOf(60000), "Pago simulado")));
        when(orders.findWithLines(any())).thenAnswer(inv -> {
            var order = new com.arquixpress.marketplace.orders.OrderEntity(inv.getArgument(0), buyerId);
            order.addLine(productId, 3, BigDecimal.valueOf(20000));
            return Optional.of(order);
        });
        when(products.findAllById(any())).thenReturn(List.of(product));

        CheckoutRequest request = request(productId, 3);
        checkoutService.checkout(buyerId, request, key);

        verify(products).releaseStock(any(UUID.class), eq(3));
    }

    @Test
    void product_shouldClampNegativeStockToZero() {
        Product product = new Product(UUID.randomUUID(), "Producto", "Desc", "cat", "http://img.png", BigDecimal.valueOf(1000), -5);
        assertEquals(0, product.stockAvailable());
    }

    @Test
    void product_updateStock_shouldClampNegativeToZero() {
        Product product = new Product(UUID.randomUUID(), "Producto", "Desc", "cat", "http://img.png", BigDecimal.valueOf(1000), 10);
        product.updateStock(-3);
        assertEquals(0, product.stockAvailable());
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
