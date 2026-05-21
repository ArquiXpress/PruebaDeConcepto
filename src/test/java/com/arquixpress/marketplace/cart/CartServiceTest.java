package com.arquixpress.marketplace.cart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CartServiceTest {

    private CartRepository cartRepo;
    private com.arquixpress.marketplace.catalog.ProductRepository productRepo;
    private CartService cartService;

    @BeforeEach
    void setUp() {
        cartRepo = mock(CartRepository.class);
        productRepo = mock(com.arquixpress.marketplace.catalog.ProductRepository.class);
        cartService = new CartService(cartRepo, productRepo);
    }

    @Test
    void addToCart_shouldCallRepository() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        cartService.addToCart(userId, productId, 2);

        verify(cartRepo).add(eq(userId), eq(productId), eq(2));
    }

    @Test
    void addToCart_withInvalidQuantity_shouldThrow() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () -> cartService.addToCart(userId, productId, 0));
    }

    @Test
    void updateQuantity_shouldCallRepository() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        cartService.updateQuantity(userId, productId, 3);

        verify(cartRepo).update(eq(userId), eq(productId), eq(3));
    }

    @Test
    void updateQuantity_withInvalid_shouldThrow() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () -> cartService.updateQuantity(userId, productId, 0));
    }

    @Test
    void removeFromCart_shouldCallRepository() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        cartService.removeFromCart(userId, productId);

        verify(cartRepo).remove(eq(userId), eq(productId));
    }

    @Test
    void getCart_shouldReturnTotals() {
        UUID userId = UUID.randomUUID();
        UUID p1 = UUID.randomUUID();

        when(cartRepo.findByUserId(userId)).thenReturn(List.of(
                new CartItem(p1, 2, new BigDecimal("10000.00"))
        ));

        var response = cartService.getCart(userId);

        assertEquals(1, response.items().size());
        assertEquals(new BigDecimal("20000.00"), response.total());
    }
}
