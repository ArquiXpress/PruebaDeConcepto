package com.arquixpress.marketplace.admin;

import com.arquixpress.marketplace.catalog.Product;
import com.arquixpress.marketplace.catalog.ProductRepository;
import com.arquixpress.marketplace.catalog.ProductStatus;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RF34ProductModerationTest {

    private ProductRepository products;
    private AdminProductModerationService service;

    @BeforeEach
    void setUp() {
        products = mock(ProductRepository.class);
        service = new AdminProductModerationService(products);
    }

    @Test
    void adminCanSuspendRejectAndApproveProductPublication() {
        UUID productId = UUID.randomUUID();
        Product product = new Product(UUID.randomUUID(), "Camara", "Desc", "tecnologia", "http://img.png",
                new BigDecimal("150000.00"), 4);
        when(products.findById(productId)).thenReturn(Optional.of(product));
        when(products.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.suspend(productId).status()).isEqualTo(ProductStatus.SUSPENDED);
        assertThat(service.reject(productId).status()).isEqualTo(ProductStatus.INACTIVE);
        assertThat(service.approve(productId).status()).isEqualTo(ProductStatus.ACTIVE);
    }

    @Test
    void moderationFailsWhenProductDoesNotExist() {
        UUID productId = UUID.randomUUID();
        when(products.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.suspend(productId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Producto no encontrado");
    }
}
