package com.arquixpress.marketplace.orders;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class OrderEntityTest {

    @Test
    void newOrder_shouldStartAsPendingPaymentWithPreparationShipment() {
        OrderEntity order = new OrderEntity(UUID.randomUUID(), UUID.randomUUID());

        assertEquals(OrderStatus.PENDING_PAYMENT, order.status());
        assertEquals(ShipmentStatus.PREPARATION, order.shipmentStatus());
        assertEquals(BigDecimal.ZERO, order.total());
        assertTrue(order.lines().isEmpty());
    }

    @Test
    void addLine_shouldAccumulateTotal() {
        OrderEntity order = new OrderEntity(UUID.randomUUID(), UUID.randomUUID());

        order.addLine(UUID.randomUUID(), 2, BigDecimal.valueOf(15000));
        order.addLine(UUID.randomUUID(), 1, BigDecimal.valueOf(8000));

        assertEquals(BigDecimal.valueOf(38000), order.total());
        assertEquals(2, order.lines().size());
    }

    @Test
    void addLine_shouldSnapshotUnitPrice() {
        OrderEntity order = new OrderEntity(UUID.randomUUID(), UUID.randomUUID());
        UUID productId = UUID.randomUUID();
        BigDecimal priceAtCheckout = BigDecimal.valueOf(25000);

        order.addLine(productId, 3, priceAtCheckout);

        OrderLine line = order.lines().get(0);
        assertEquals(productId, line.productId());
        assertEquals(3, line.quantity());
        assertEquals(priceAtCheckout, line.unitPrice());
    }

    @Test
    void markPaid_shouldTransitionToPaid() {
        OrderEntity order = new OrderEntity(UUID.randomUUID(), UUID.randomUUID());
        order.markPaid();

        assertEquals(OrderStatus.PAID, order.status());
    }

    @Test
    void markRejected_shouldTransitionToPaymentRejected() {
        OrderEntity order = new OrderEntity(UUID.randomUUID(), UUID.randomUUID());
        order.markRejected();

        assertEquals(OrderStatus.PAYMENT_REJECTED, order.status());
    }

    @Test
    void markPendingPayment_shouldResetToPending() {
        OrderEntity order = new OrderEntity(UUID.randomUUID(), UUID.randomUUID());
        order.markRejected();
        order.markPendingPayment();

        assertEquals(OrderStatus.PENDING_PAYMENT, order.status());
    }

    @Test
    void updateShipment_shouldAdvanceForwardWhenPaid() {
        OrderEntity order = new OrderEntity(UUID.randomUUID(), UUID.randomUUID());
        order.markPaid();

        order.updateShipment(ShipmentStatus.IN_ROUTE);
        assertEquals(ShipmentStatus.IN_ROUTE, order.shipmentStatus());

        order.updateShipment(ShipmentStatus.DELIVERED);
        assertEquals(ShipmentStatus.DELIVERED, order.shipmentStatus());
    }

    @Test
    void updateShipment_shouldThrowWhenSkippingBackwardState() {
        OrderEntity order = new OrderEntity(UUID.randomUUID(), UUID.randomUUID());
        order.markPaid();
        order.updateShipment(ShipmentStatus.IN_ROUTE);
        order.updateShipment(ShipmentStatus.DELIVERED);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> order.updateShipment(ShipmentStatus.PREPARATION)
        );
        assertEquals("Solo se permite avanzar o retroceder un paso logistico", ex.getMessage());
    }

    @Test
    void updateShipment_shouldThrowWhenOrderNotPaid() {
        OrderEntity order = new OrderEntity(UUID.randomUUID(), UUID.randomUUID());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> order.updateShipment(ShipmentStatus.IN_ROUTE)
        );
        assertEquals("Solo pedidos pagados pueden cambiar estado logistico", ex.getMessage());
    }

    @Test
    void updateShipment_shouldThrowWhenRejectedOrderTriesToAdvance() {
        OrderEntity order = new OrderEntity(UUID.randomUUID(), UUID.randomUUID());
        order.markRejected();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> order.updateShipment(ShipmentStatus.IN_ROUTE)
        );
        assertEquals("Solo pedidos pagados pueden cambiar estado logistico", ex.getMessage());
    }

    @Test
    void updateShipment_shouldAllowSameStateTransition() {
        OrderEntity order = new OrderEntity(UUID.randomUUID(), UUID.randomUUID());
        order.markPaid();

        order.updateShipment(ShipmentStatus.PREPARATION);
        assertEquals(ShipmentStatus.PREPARATION, order.shipmentStatus());
    }

    @Test
    void assignCenter_shouldSetLogisticsFields() {
        OrderEntity order = new OrderEntity(UUID.randomUUID(), UUID.randomUUID());
        UUID centerId = UUID.randomUUID();
        UUID operatorId = UUID.randomUUID();

        order.assignCenter(centerId, operatorId);

        assertEquals(centerId, order.logisticsCenterId());
        assertEquals(operatorId, order.logisticsOperatorId());
    }
}
