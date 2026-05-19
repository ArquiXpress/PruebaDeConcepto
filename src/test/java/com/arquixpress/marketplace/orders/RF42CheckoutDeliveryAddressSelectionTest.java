package com.arquixpress.marketplace.orders;

import com.arquixpress.marketplace.addresses.DeliveryAddress;
import com.arquixpress.marketplace.addresses.DeliveryAddressRepository;
import com.arquixpress.marketplace.catalog.Product;
import com.arquixpress.marketplace.catalog.ProductRepository;
import com.arquixpress.marketplace.catalog.ProductStatus;
import com.arquixpress.marketplace.logistics.LogisticsCenterRepository;
import com.arquixpress.marketplace.notifications.NotificationOutboxRepository;
import com.arquixpress.marketplace.notifications.NotificationService;
import com.arquixpress.marketplace.payments.PaymentGatewayClient;
import com.arquixpress.marketplace.payments.PaymentGatewayResult;
import com.arquixpress.marketplace.payments.PaymentTransaction;
import com.arquixpress.marketplace.payments.PaymentTransactionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
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
import static org.mockito.Mockito.when;

class RF42CheckoutDeliveryAddressSelectionTest {

    private ProductRepository products;
    private OrderRepository orders;
    private PaymentTransactionRepository payments;
    private PaymentGatewayClient paymentGateway;
    private DeliveryAddressRepository addresses;
    private CheckoutService service;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        products = mock(ProductRepository.class);
        orders = mock(OrderRepository.class);
        payments = mock(PaymentTransactionRepository.class);
        paymentGateway = mock(PaymentGatewayClient.class);
        addresses = mock(DeliveryAddressRepository.class);
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
        service = new CheckoutService(products, orders, payments, paymentGateway,
                mock(NotificationOutboxRepository.class), mock(NotificationService.class),
                mock(LogisticsCenterRepository.class), tx, addresses);
    }

    @Test
    void checkoutUsesPreviouslyRegisteredDeliveryAddress() {
        UUID buyerId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();
        String key = "address-key";
        Product product = new Product(UUID.randomUUID(), "Producto", "Desc", "cat", "http://img.png",
                new BigDecimal("30000.00"), 5);
        DeliveryAddress address = new DeliveryAddress(addressId, buyerId, "Casa", "Cliente",
                "Calle 1 # 2-3", "Bogota", "3000000000");
        AtomicReference<OrderEntity> savedOrder = new AtomicReference<>();
        AtomicReference<PaymentTransaction> savedPayment = new AtomicReference<>();

        when(payments.findByIdempotencyKey(key)).thenAnswer(invocation ->
                savedPayment.get() == null ? Optional.empty() : Optional.of(savedPayment.get()));
        when(addresses.findByIdAndUserIdAndActiveTrue(addressId, buyerId)).thenReturn(Optional.of(address));
        when(products.findByIdAndStatus(product.id(), ProductStatus.ACTIVE)).thenReturn(Optional.of(product));
        when(products.reserveStock(product.id(), 1)).thenReturn(1);
        when(orders.save(any(OrderEntity.class))).thenAnswer(invocation -> {
            savedOrder.set(invocation.getArgument(0));
            return savedOrder.get();
        });
        when(payments.save(any(PaymentTransaction.class))).thenAnswer(invocation -> {
            savedPayment.set(invocation.getArgument(0));
            return savedPayment.get();
        });
        when(paymentGateway.charge(any(), any(), eq(key))).thenReturn(PaymentGatewayResult.approved("GW-ADDRESS"));
        when(orders.findWithLines(any())).thenAnswer(invocation -> Optional.of(savedOrder.get()));
        when(products.findAllById(any())).thenReturn(List.of(product));

        CheckoutResponse response = service.checkout(buyerId,
                new CheckoutRequest(List.of(new CheckoutItemRequest(product.id(), 1)), "PSE", addressId),
                key);

        assertThat(response.deliveryAddressId()).isEqualTo(addressId);
        assertThat(response.deliveryAddressSnapshot()).contains("Calle 1 # 2-3", "Bogota");
        assertThat(savedOrder.get().deliveryAddressId()).isEqualTo(addressId);
    }

    @Test
    void checkoutRejectsAddressThatDoesNotBelongToBuyer() {
        UUID buyerId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();
        String key = "bad-address-key";
        when(payments.findByIdempotencyKey(key)).thenReturn(Optional.empty());
        when(addresses.findByIdAndUserIdAndActiveTrue(addressId, buyerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.checkout(buyerId,
                new CheckoutRequest(List.of(new CheckoutItemRequest(UUID.randomUUID(), 1)), "PSE", addressId),
                key))
                .isInstanceOf(CheckoutProblem.class)
                .hasMessageContaining("direccion de entrega");
    }
}
