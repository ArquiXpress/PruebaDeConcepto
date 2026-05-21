package com.arquixpress.marketplace.orders;

import com.arquixpress.marketplace.catalog.Product;
import com.arquixpress.marketplace.catalog.ProductRepository;
import com.arquixpress.marketplace.identity.AppUser;
import com.arquixpress.marketplace.identity.AppUserRepository;
import com.arquixpress.marketplace.identity.Role;
import com.arquixpress.marketplace.logistics.LogisticsCenter;
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
import org.mockito.ArgumentCaptor;
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
 * RF-42: Seleccionar dirección registrada en checkout
 */
class CheckoutAddressTest {

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

        when(users.findAllById(any())).thenReturn(List.of());

        checkoutService = new CheckoutService(products, orders, payments, paymentGateway, outbox, notifications,
                centers, users, coupons, redemptions, tx);
    }

    @Test
    void checkout_shouldUseAddressFromRequestWhenProvided() {
        UUID buyerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        String idempotencyKey = "key-addr-001";

        Product product = new Product(UUID.randomUUID(), "Prod", "Desc", "cat", "http://img.png",
                BigDecimal.valueOf(50000), 5);

        when(payments.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(centers.findAll()).thenReturn(List.of());
        when(products.findById(any(UUID.class))).thenReturn(Optional.of(product));
        when(products.reserveStock(any(UUID.class), eq(1))).thenReturn(1);
        when(orders.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(payments.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentGateway.charge(any(UUID.class), any(BigDecimal.class), eq(idempotencyKey)))
                .thenReturn(PaymentGatewayResult.approved("REF-ADDR"));

        PaymentTransaction pt = new PaymentTransaction(UUID.randomUUID(), UUID.randomUUID(),
                idempotencyKey, BigDecimal.valueOf(50000), "Pago simulado");
        when(payments.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(pt));
        when(orders.findWithLines(any(UUID.class))).thenAnswer(inv -> {
            OrderEntity order = new OrderEntity(inv.getArgument(0), buyerId);
            order.addLine(productId, 1, BigDecimal.valueOf(50000));
            order.setShipping("Carrera 10 #20-30", "Medellin", BigDecimal.ZERO);
            order.markPaid();
            return Optional.of(order);
        });
        when(products.findAllById(any())).thenReturn(List.of(product));
        when(users.findById(buyerId)).thenReturn(Optional.empty());

        ArgumentCaptor<OrderEntity> captor = ArgumentCaptor.forClass(OrderEntity.class);

        checkoutService.checkout(buyerId,
                new CheckoutRequest(List.of(new CheckoutItemRequest(productId, 1)), null,
                        "Carrera 10 #20-30", "Medellin"),
                idempotencyKey);

        verify(orders).save(captor.capture());
        assertEquals("Carrera 10 #20-30", captor.getValue().shippingAddress());
        assertEquals("Medellin", captor.getValue().shippingCity());
    }

    @Test
    void checkout_shouldFallbackToUserProfileAddressWhenNotInRequest() {
        UUID buyerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        String idempotencyKey = "key-addr-002";

        Product product = new Product(UUID.randomUUID(), "Prod", "Desc", "cat", "http://img.png",
                BigDecimal.valueOf(50000), 5);
        AppUser buyer = AppUser.create(buyerId, "buyer@mail.com", "pass", "Buyer", Set.of(Role.CLIENT));
        buyer.setAddress("Calle Perfil 99");
        buyer.setCity("Cali");

        when(payments.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(centers.findAll()).thenReturn(List.of());
        when(products.findById(any(UUID.class))).thenReturn(Optional.of(product));
        when(products.reserveStock(any(UUID.class), eq(1))).thenReturn(1);
        when(orders.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(payments.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentGateway.charge(any(UUID.class), any(BigDecimal.class), eq(idempotencyKey)))
                .thenReturn(PaymentGatewayResult.approved("REF-PROFILE"));

        PaymentTransaction pt = new PaymentTransaction(UUID.randomUUID(), UUID.randomUUID(),
                idempotencyKey, BigDecimal.valueOf(50000), "Pago simulado");
        when(payments.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(pt));
        when(orders.findWithLines(any(UUID.class))).thenAnswer(inv -> {
            OrderEntity order = new OrderEntity(inv.getArgument(0), buyerId);
            order.addLine(productId, 1, BigDecimal.valueOf(50000));
            order.setShipping("Calle Perfil 99", "Cali", BigDecimal.ZERO);
            order.markPaid();
            return Optional.of(order);
        });
        when(products.findAllById(any())).thenReturn(List.of(product));
        when(users.findById(buyerId)).thenReturn(Optional.of(buyer));

        ArgumentCaptor<OrderEntity> captor = ArgumentCaptor.forClass(OrderEntity.class);

        checkoutService.checkout(buyerId,
                new CheckoutRequest(List.of(new CheckoutItemRequest(productId, 1)), null, null, null),
                idempotencyKey);

        verify(orders).save(captor.capture());
        assertEquals("Calle Perfil 99", captor.getValue().shippingAddress());
        assertEquals("Cali", captor.getValue().shippingCity());
    }

    @Test
    void checkout_shouldFailWhenNoAddressAvailable() {
        UUID buyerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        String idempotencyKey = "key-addr-003";

        Product product = new Product(UUID.randomUUID(), "Prod", "Desc", "cat", "http://img.png",
                BigDecimal.valueOf(50000), 5);

        when(payments.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(centers.findAll()).thenReturn(List.of());
        when(products.findById(any(UUID.class))).thenReturn(Optional.of(product));
        when(products.reserveStock(any(UUID.class), eq(1))).thenReturn(1);
        when(users.findById(buyerId)).thenReturn(Optional.empty());

        CheckoutProblem ex = assertThrows(CheckoutProblem.class,
                () -> checkoutService.checkout(buyerId,
                        new CheckoutRequest(List.of(new CheckoutItemRequest(productId, 1)), null, null, null),
                        idempotencyKey));
        assertEquals("SHIPPING_ADDRESS_REQUIRED", ex.code());
    }

    @Test
    void checkout_shouldNormalizeAccentedCityName() {
        UUID buyerId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID bogotaCenterId = UUID.randomUUID();
        String idempotencyKey = "key-addr-004";

        Product product = new Product(sellerId, "Prod", "Desc", "cat", "http://img.png",
                BigDecimal.valueOf(50000), 5);
        AppUser seller = AppUser.create(sellerId, "s@mail.com", "pass", "Seller", Set.of(Role.SELLER));
        seller.setCity("Bogotá");

        when(payments.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(centers.findAll()).thenReturn(List.of(
                new LogisticsCenter(bogotaCenterId, "Bogota", "Centro Bogota")));
        when(products.findById(any(UUID.class))).thenReturn(Optional.of(product));
        when(products.reserveStock(any(UUID.class), eq(1))).thenReturn(1);
        when(users.findById(buyerId)).thenReturn(Optional.empty());
        when(users.findAllById(any())).thenReturn(List.of(seller));
        when(orders.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(payments.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentGateway.charge(any(UUID.class), any(BigDecimal.class), eq(idempotencyKey)))
                .thenReturn(PaymentGatewayResult.approved("REF-BOGOTA"));

        PaymentTransaction pt = new PaymentTransaction(UUID.randomUUID(), UUID.randomUUID(),
                idempotencyKey, BigDecimal.valueOf(50000), "Pago simulado");
        when(payments.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(pt));
        when(orders.findWithLines(any(UUID.class))).thenAnswer(inv -> {
            OrderEntity order = new OrderEntity(inv.getArgument(0), buyerId);
            order.addLine(productId, 1, BigDecimal.valueOf(50000));
            order.assignCenter(bogotaCenterId, null);
            order.markPaid();
            return Optional.of(order);
        });
        when(products.findAllById(any())).thenReturn(List.of(product));

        ArgumentCaptor<OrderEntity> captor = ArgumentCaptor.forClass(OrderEntity.class);

        checkoutService.checkout(buyerId,
                new CheckoutRequest(List.of(new CheckoutItemRequest(productId, 1)), null,
                        "Av El Dorado", "Bogotá"),
                idempotencyKey);

        verify(orders).save(captor.capture());
        assertEquals("Bogota", captor.getValue().shippingCity());
        assertEquals(bogotaCenterId, captor.getValue().logisticsCenterId());
    }
}
