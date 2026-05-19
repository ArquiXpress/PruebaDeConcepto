package com.arquixpress.marketplace.orders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RF41SellerOrdersTest {

    @Test
    void sellerCanListOrdersAssociatedWithTheirProductsAndStatus() {
        UUID sellerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        OrderEntity order = new OrderEntity(UUID.randomUUID(), UUID.randomUUID());
        order.addLine(productId, 3, new BigDecimal("22000.00"));
        order.markPaid();
        order.updateShipment(ShipmentStatus.IN_ROUTE);
        OrderRepository orders = mock(OrderRepository.class);
        when(orders.findBySellerProductWithLines(sellerId)).thenReturn(List.of(order));
        SellerOrderService service = new SellerOrderService(orders);

        List<OrderResponse> result = service.listForSeller(sellerId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(OrderStatus.PAID);
        assertThat(result.get(0).shipmentStatus()).isEqualTo(ShipmentStatus.IN_ROUTE);
        assertThat(result.get(0).lines()).extracting(OrderLineResponse::productId).containsExactly(productId);
        verify(orders).findBySellerProductWithLines(sellerId);
    }
}
