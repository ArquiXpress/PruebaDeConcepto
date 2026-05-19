package com.arquixpress.marketplace.catalog.application;

import com.arquixpress.marketplace.catalog.Product;
import com.arquixpress.marketplace.catalog.ProductRepository;
import com.arquixpress.marketplace.catalog.api.SellerProductRequest;
import com.arquixpress.marketplace.catalog.api.SellerProductResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SellerProductServiceTest {

    @Mock
    ProductRepository products;

    SellerProductService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new SellerProductService(products);
    }

    @Test
    void crearPublicacion_success() {
        UUID sellerId = UUID.randomUUID();
        SellerProductRequest req = new SellerProductRequest("Titulo", "Desc", "cat", "http://img/1.png", new BigDecimal("9.99"), 10, null);
        when(products.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        SellerProductResponse resp = service.create(sellerId, req);

        assertThat(resp).isNotNull();
        assertThat(resp.sellerId()).isEqualTo(sellerId);
        verify(products).save(any(Product.class));
    }

    @Test
    void actualizarPublicacion_updatesFields() {
        UUID sellerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Product existing = new Product(sellerId, "T", "D", "c", "http://img/1.png", new BigDecimal("1.0"), 5);
        when(products.findByIdAndSellerId(productId, sellerId)).thenReturn(Optional.of(existing));

        SellerProductRequest req = new SellerProductRequest("Nuevo", "Desc2", "cat2", "http://img/2.png", new BigDecimal("5.00"), 2, null);
        var resp = service.update(sellerId, productId, req);

        assertThat(resp.title()).isEqualTo("Nuevo");
        assertThat(resp.price()).isEqualTo(new BigDecimal("5.00"));
    }

    @Test
    void pausarYActivar_cambiaEstadoCorrectamente() {
        UUID sellerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Product p = new Product(sellerId, "T", "D", "c", "http://img/1.png", new BigDecimal("1.0"), 5);
        when(products.findByIdAndSellerId(productId, sellerId)).thenReturn(Optional.of(p));

        var deactivated = service.deactivate(sellerId, productId);
        assertThat(deactivated.status()).isEqualTo(com.arquixpress.marketplace.catalog.ProductStatus.INACTIVE);

        var activated = service.activate(sellerId, productId);
        assertThat(activated.status()).isEqualTo(com.arquixpress.marketplace.catalog.ProductStatus.ACTIVE);
    }

    @Test
    void actualizarStock_edgeCases() {
        UUID sellerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Product p = new Product(sellerId, "T", "D", "c", "http://img/1.png", new BigDecimal("1.0"), 5);
        when(products.findByIdAndSellerId(productId, sellerId)).thenReturn(Optional.of(p));

        var resp = service.updateStock(sellerId, productId, 0);
        assertThat(resp.stockAvailable()).isEqualTo(0);

        resp = service.updateStock(sellerId, productId, -5);
        assertThat(resp.stockAvailable()).isEqualTo(0);
    }
    
    @Test
    void updateStock_deberiaLanzarExcepcionSiProductoNoPerteneceAlVendedor() {
        UUID sellerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        when(products.findByIdAndSellerId(productId, sellerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStock(sellerId, productId, 50))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no encontrado");
    }

    @Test
    void listMine_deberiaRetornarSoloProductosDelVendedor() {
        UUID sellerId = UUID.randomUUID();
        Product p1 = new Product(sellerId, "Prod A", "Desc", "cat",
                "http://img/1.png", new BigDecimal("10.00"), 5);
        Product p2 = new Product(sellerId, "Prod B", "Desc", "cat",
                "http://img/2.png", new BigDecimal("20.00"), 3);

        when(products.findBySellerIdOrderByCreatedAtDesc(sellerId)).thenReturn(List.of(p1, p2));

        var resultado = service.listMine(sellerId);

        assertThat(resultado).hasSize(2);
        assertThat(resultado).allMatch(r -> r.sellerId().equals(sellerId));
    }

    @Test
    void detailMine_deberiaLanzarExcepcionSiProductoNoPerteneceAlVendedor() {
        UUID sellerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        when(products.findByIdAndSellerId(productId, sellerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.detailMine(sellerId, productId))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
