package com.arquixpress.marketplace.orders;

import com.arquixpress.marketplace.catalog.Product;
import com.arquixpress.marketplace.identity.AppUser;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record OrderResponse(
        UUID orderId,
        OrderStatus status,
        ShipmentStatus shipmentStatus,
        BigDecimal total,
        BigDecimal shippingCost,
        String shippingAddress,
        String shippingCity,
        UUID logisticsCenterId,
        UUID logisticsOperatorId,
        UUID buyerId,
        String buyerName,
        String buyerEmail,
        List<OrderLineResponse> lines) {

    static OrderResponse from(OrderEntity order) {
        return from(order, Map.of(), Map.of(), null);
    }

    static OrderResponse from(OrderEntity order, Map<UUID, Product> productById, Map<UUID, AppUser> sellerById,
            AppUser buyer) {
        return new OrderResponse(
                order.id(),
                order.status(),
                order.shipmentStatus(),
                order.total(),
                order.shippingCost(),
                order.shippingAddress(),
                order.shippingCity(),
                order.logisticsCenterId(),
                order.logisticsOperatorId(),
                order.buyerId(),
                buyer == null ? "" : buyer.displayName(),
                buyer == null ? "" : buyer.email(),
                order.lines().stream()
                        .map(line -> {
                            Product product = productById.get(line.productId());
                            AppUser seller = product == null ? null : sellerById.get(product.sellerId());
                            return OrderLineResponse.from(line, product, seller);
                        })
                        .toList());
    }
}
