package com.arquixpress.marketplace.orders;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SellerOrderService {
    private final OrderRepository orders;

    public SellerOrderService(OrderRepository orders) {
        this.orders = orders;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listForSeller(UUID sellerId) {
        return orders.findBySellerProductWithLines(sellerId).stream()
                .map(OrderResponse::from)
                .toList();
    }
}
