package com.arquixpress.marketplace.favorites;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.arquixpress.marketplace.catalog.Product;
import com.arquixpress.marketplace.catalog.ProductRepository;

class FavoriteServiceTest {

    private FavoriteRepository favorites;
    private ProductRepository products;
    private FavoriteService service;

    @BeforeEach
    void setUp() {
        favorites = mock(FavoriteRepository.class);
        products = mock(ProductRepository.class);
        service = new FavoriteService(favorites, products);
    }

    @Test
    void addFavorite_shouldCallRepository() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        service.addFavorite(userId, productId);

        verify(favorites).save(eq(userId), eq(productId));
    }

    @Test
    void removeFavorite_shouldCallRepository() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        service.removeFavorite(userId, productId);

        verify(favorites).delete(eq(userId), eq(productId));
    }

    @Test
    void listFavorites_shouldReturnProductSummariesForActiveProducts() {
        UUID userId = UUID.randomUUID();
        Product p = createProduct("Taladro", "Taladro industrial");

        when(favorites.findProductIdsByUserId(userId)).thenReturn(List.of(p.id()));
        when(products.findByIdAndStatus(p.id(), com.arquixpress.marketplace.catalog.ProductStatus.ACTIVE))
                .thenReturn(Optional.of(p));

        var list = service.listFavorites(userId);

        assertEquals(1, list.size());
        assertEquals("Taladro", list.get(0).title());
    }

    private Product createProduct(String title, String description) {
        return new Product(
                UUID.randomUUID(),
                title,
                description,
                "herramientas",
                "https://example.com/img.png",
                new BigDecimal("100000"),
                5
        );
    }
}
