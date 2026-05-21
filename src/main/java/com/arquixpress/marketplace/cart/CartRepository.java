package com.arquixpress.marketplace.cart;

import java.util.List;
import java.util.UUID;

public interface CartRepository {
    void add(UUID userId, UUID productId, int quantity);
    void remove(UUID userId, UUID productId);
    void update(UUID userId, UUID productId, int quantity);
    List<CartItem> findByUserId(UUID userId);
}
