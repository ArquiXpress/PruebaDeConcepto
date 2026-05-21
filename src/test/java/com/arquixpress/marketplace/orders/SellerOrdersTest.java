package com.arquixpress.marketplace.orders;

import com.arquixpress.marketplace.catalog.Product;
import com.arquixpress.marketplace.catalog.ProductRepository;
import com.arquixpress.marketplace.identity.AppUser;
import com.arquixpress.marketplace.identity.AppUserRepository;
import com.arquixpress.marketplace.identity.Role;
import com.arquixpress.marketplace.logistics.LogisticsCenterRepository;
import com.arquixpress.marketplace.notifications.NotificationOutboxRepository;
import com.arquixpress.marketplace.notifications.NotificationService;
import com.arquixpress.marketplace.payments.PaymentGatewayClient;
import com.arquixpress.marketplace.payments.PaymentGatewayResult;
import com.arquixpress.marketplace.payments.PaymentTransaction;
import com.arquixpress.marketplace.payments.PaymentTransactionRepository;
import com.arquixpress.marketplace.promotions.CouponRedemptionRepository;
import com.arquixpress.marketplace.promotions.MarketingCouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/*
 * RF-41: Consultar pedidos asociados al vendedor
 */
class SellerOrdersTest {

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
            TransactionCallback<?> callback = inv.getArgument(0);
            callback.doInTransaction(null);
            return null;
        }).when(tx).executeWithoutResult(any());

        when(users.findById(any(UUID.class))).thenReturn(Optional.empty());
        when(users.findAllById(any())).thenReturn(List.of());

        checkoutService = new CheckoutService(products, orders, payments, paymentGateway, outbox, notifications,
                centers, users, coupons, redemptions, tx);
    }

    @Test
    void sellerShouldReceiveNotificationWhenProductIsSold() {
        UUID buyerId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        String idempotencyKey = "key-seller-notify";

        Product product = new Product(sellerId, "Producto Vendedor", "Desc", "cat",
                "http://img.png", BigDecimal.valueOf(60000), 8);

        AppUser seller = AppUser.create(sellerId, "seller@mail.com", "pass", "Vendedor", Set.of(Role.SELLER));
        seller.setCity("Bogota");

        when(payments.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(centers.findAll()).thenReturn(List.of());
        when(products.findById(any(UUID.class))).thenReturn(Optional.of(product));
        when(products.reserveStock(any(UUID.class), eq(1))).thenReturn(1);
        when(orders.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(payments.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentGateway.charge(any(UUID.class), any(BigDecimal.class), eq(idempotencyKey)))
                .thenReturn(PaymentGatewayResult.approved("REF-SALE"));

        PaymentTransaction pt = new PaymentTransaction(UUID.randomUUID(), UUID.randomUUID(),
                idempotencyKey, BigDecimal.valueOf(60000), "Pago simulado");
        when(payments.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(pt));
        when(orders.findWithLines(any(UUID.class))).thenAnswer(inv -> {
            OrderEntity order = new OrderEntity(inv.getArgument(0), buyerId);
            order.addLine(productId, 1, BigDecimal.valueOf(60000));
            order.markPaid();
            return Optional.of(order);
        });
        when(products.findAllById(any())).thenReturn(List.of(product));
        when(users.findAllById(any())).thenReturn(List.of(seller));

        checkoutService.checkout(buyerId,
                new CheckoutRequest(List.of(new CheckoutItemRequest(productId, 1)), null, "Calle 1", "Bogota"),
                idempotencyKey);

        verify(notifications).notify(eq(sellerId), eq("SELLER_SALE"), anyString(), anyString(), anyString());
    }

    @Test
    void sellerShouldReceiveOneNotificationPerSeller() {
        UUID buyerId = UUID.randomUUID();
        UUID sellerId1 = UUID.randomUUID();
        UUID sellerId2 = UUID.randomUUID();
        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();
        String idempotencyKey = "key-two-sellers";

        Product product1 = new Product(sellerId1, "Prod1", "Desc", "cat", "http://img.png",
                BigDecimal.valueOf(20000), 5);
        Product product2 = new Product(sellerId2, "Prod2", "Desc", "cat", "http://img.png",
                BigDecimal.valueOf(30000), 5);

        when(payments.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(centers.findAll()).thenReturn(List.of());
        when(products.findById(productId1)).thenReturn(Optional.of(product1));
        when(products.findById(productId2)).thenReturn(Optional.of(product2));
        when(products.reserveStock(any(UUID.class), eq(1))).thenReturn(1);
        when(orders.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(payments.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentGateway.charge(any(UUID.class), any(BigDecimal.class), eq(idempotencyKey)))
                .thenReturn(PaymentGatewayResult.approved("REF-TWO"));

        PaymentTransaction pt = new PaymentTransaction(UUID.randomUUID(), UUID.randomUUID(),
                idempotencyKey, BigDecimal.valueOf(50000), "Pago simulado");
        when(payments.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(pt));
        when(orders.findWithLines(any(UUID.class))).thenAnswer(inv -> {
            OrderEntity order = new OrderEntity(inv.getArgument(0), buyerId);
            order.addLine(productId1, 1, BigDecimal.valueOf(20000));
            order.addLine(productId2, 1, BigDecimal.valueOf(30000));
            order.markPaid();
            return Optional.of(order);
        });
        when(products.findAllById(any())).thenReturn(List.of(product1, product2));
        when(users.findAllById(any())).thenReturn(List.of());

        checkoutService.checkout(buyerId,
                new CheckoutRequest(
                        List.of(new CheckoutItemRequest(productId1, 1), new CheckoutItemRequest(productId2, 1)),
                        null, "Calle 1", "Bogota"),
                idempotencyKey);

        verify(notifications).notify(eq(sellerId1), eq("SELLER_SALE"), anyString(), anyString(), anyString());
        verify(notifications).notify(eq(sellerId2), eq("SELLER_SALE"), anyString(), anyString(), anyString());
    }

    @Test
    void getOrder_shouldReturnOrderWithBuyerInfo() {
        UUID orderId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();

        OrderEntity order = new OrderEntity(orderId, buyerId);
        order.addLine(UUID.randomUUID(), 1, BigDecimal.valueOf(50000));
        order.markPaid();

        AppUser buyer = AppUser.create(buyerId, "buyer@mail.com", "pass", "Comprador", Set.of(Role.CLIENT));

        when(orders.findWithLines(orderId)).thenReturn(Optional.of(order));
        when(products.findAllById(any())).thenReturn(List.of());
        when(users.findById(buyerId)).thenReturn(Optional.of(buyer));
        when(users.findAllById(any())).thenReturn(List.of());

        OrderResponse result = checkoutService.getOrder(orderId);

        assertNotNull(result);
        assertEquals(orderId, result.orderId());
        assertEquals(OrderStatus.PAID, result.status());
        assertEquals("Comprador", result.buyerName());
    }

    @Test
    void getOrdersForBuyer_shouldReturnAllOrdersForBuyer() {
        UUID buyerId = UUID.randomUUID();

        OrderEntity order1 = new OrderEntity(UUID.randomUUID(), buyerId);
        order1.addLine(UUID.randomUUID(), 1, BigDecimal.valueOf(20000));
        OrderEntity order2 = new OrderEntity(UUID.randomUUID(), buyerId);
        order2.addLine(UUID.randomUUID(), 2, BigDecimal.valueOf(15000));
        order2.markPaid();

        when(orders.findByBuyerWithLines(buyerId)).thenReturn(List.of(order1, order2));
        when(products.findAllById(any())).thenReturn(List.of());
        when(users.findAllById(any())).thenReturn(List.of());
        when(users.findById(buyerId)).thenReturn(Optional.empty());

        List<OrderResponse> result = checkoutService.getOrdersForBuyer(buyerId);

        assertEquals(2, result.size());
    }
}
