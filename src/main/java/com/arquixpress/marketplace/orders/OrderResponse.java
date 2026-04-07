package com.arquixpress.marketplace.orders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderResponse(UUID orderId, OrderStatus status, ShipmentStatus shipmentStatus, BigDecimal total, List<OrderLineResponse> lines) {
    static OrderResponse from(OrderEntity order) {
        return new OrderResponse(
                order.id(),
                order.status(),
                order.shipmentStatus(),
                order.total(),
                order.lines().stream().map(OrderLineResponse::from).toList());
    }
}
