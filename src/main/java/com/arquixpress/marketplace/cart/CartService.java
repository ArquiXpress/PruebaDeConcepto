package com.arquixpress.marketplace.cart;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.arquixpress.marketplace.catalog.ProductRepository;

public class CartService {

    private final CartRepository cart;
    private final ProductRepository products;

    public CartService(CartRepository cart, ProductRepository products) {
        this.cart = cart;
        this.products = products;
    }

    public void addToCart(UUID userId, UUID productId, int quantity) {
        if (quantity < 1) throw new IllegalArgumentException("Cantidad debe ser al menos 1");
        cart.add(userId, productId, quantity);
    }

    public void removeFromCart(UUID userId, UUID productId) {
        cart.remove(userId, productId);
    }

    public void updateQuantity(UUID userId, UUID productId, int quantity) {
        if (quantity < 1) throw new IllegalArgumentException("Cantidad debe ser al menos 1");
        cart.update(userId, productId, quantity);
    }

    public CartResponse getCart(UUID userId) {
        List<CartItem> items = cart.findByUserId(userId);
        List<CartItemResponse> responses = items.stream().map(it -> {
            BigDecimal subtotal = it.unitPrice().multiply(BigDecimal.valueOf(it.quantity()));
            return new CartItemResponse(it.productId(), it.quantity(), it.unitPrice(), subtotal);
        }).collect(Collectors.toList());

        BigDecimal total = responses.stream()
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(responses, total);
    }
}
