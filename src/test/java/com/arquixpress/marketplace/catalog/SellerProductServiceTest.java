package com.arquixpress.marketplace.catalog;

import com.arquixpress.marketplace.catalog.api.ModerationRequest;
import com.arquixpress.marketplace.catalog.application.SellerProductService;
import com.arquixpress.marketplace.identity.AppUserRepository;
import com.arquixpress.marketplace.notifications.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/*
 * RF-22: Gestión inventario vendedor
 * RF-34: Moderación administrativa
 */
class SellerProductServiceTest {

    private ProductRepository productRepository;
    private NotificationService notifications;
    private AppUserRepository users;
    private SellerProductService service;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        notifications = mock(NotificationService.class);
        users = mock(AppUserRepository.class);
        service = new SellerProductService(productRepository, notifications, users);
    }

    // RF-22: Actualizar inventario vendedor

    @Test
    void updateStock_shouldUpdateStockForSellerProduct() {
        UUID sellerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Product product = new Product(sellerId, "Mesa", "Desc", "muebles", "http://img.png",
                BigDecimal.valueOf(80000), 10);

        when(productRepository.findByIdAndSellerId(productId, sellerId)).thenReturn(Optional.of(product));

        var result = service.updateStock(sellerId, productId, 25);

        assertEquals(25, result.stockAvailable());
    }

    @Test
    void updateStock_shouldSetInactiveWhenStockIsZero() {
        UUID sellerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Product product = new Product(sellerId, "Silla", "Desc", "muebles", "http://img.png",
                BigDecimal.valueOf(50000), 5);

        when(productRepository.findByIdAndSellerId(productId, sellerId)).thenReturn(Optional.of(product));

        var result = service.updateStock(sellerId, productId, 0);

        assertEquals(0, result.stockAvailable());
        assertEquals(ProductStatus.INACTIVE, result.status());
    }

    @Test
    void updateStock_shouldThrowWhenProductDoesNotBelongToSeller() {
        UUID sellerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        when(productRepository.findByIdAndSellerId(productId, sellerId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.updateStock(sellerId, productId, 10));
    }

    @Test
    void activate_shouldActivateProductWithStock() {
        UUID sellerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Product product = new Product(sellerId, "Lampara", "Desc", "decoracion", "http://img.png",
                BigDecimal.valueOf(30000), 3);
        product.deactivate();

        when(productRepository.findByIdAndSellerId(productId, sellerId)).thenReturn(Optional.of(product));

        var result = service.activate(sellerId, productId);

        assertEquals(ProductStatus.ACTIVE, result.status());
    }

    @Test
    void activate_shouldKeepInactiveWhenStockIsZero() {
        UUID sellerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Product product = new Product(sellerId, "Lampara sin stock", "Desc", "decoracion", "http://img.png",
                BigDecimal.valueOf(30000), 0);

        when(productRepository.findByIdAndSellerId(productId, sellerId)).thenReturn(Optional.of(product));

        var result = service.activate(sellerId, productId);

        assertEquals(ProductStatus.INACTIVE, result.status());
    }

    @Test
    void deactivate_shouldDeactivateProduct() {
        UUID sellerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Product product = new Product(sellerId, "Cuadro", "Desc", "arte", "http://img.png",
                BigDecimal.valueOf(120000), 2);

        when(productRepository.findByIdAndSellerId(productId, sellerId)).thenReturn(Optional.of(product));

        var result = service.deactivate(sellerId, productId);

        assertEquals(ProductStatus.INACTIVE, result.status());
    }

    // RF-34: Moderación administrativa

    @Test
    void removeByModerator_shouldSetInactiveAndRecordReason() {
        UUID moderatorId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        Product product = new Product(sellerId, "Producto Inapropiado", "Desc", "categoria", "http://img.png",
                BigDecimal.valueOf(10000), 5);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        var result = service.removeByModerator(moderatorId, productId, new ModerationRequest("Contenido inapropiado"));

        assertEquals(ProductStatus.INACTIVE, result.status());
        assertEquals("Contenido inapropiado", result.moderationReason());
        verify(notifications).notify(eq(sellerId), eq("PRODUCT_REMOVED"), anyString(), anyString(), anyString());
    }

    @Test
    void removeByModerator_shouldUseDefaultReasonWhenNoneProvided() {
        UUID moderatorId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        Product product = new Product(sellerId, "Producto", "Desc", "cat", "http://img.png",
                BigDecimal.valueOf(5000), 1);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        var result = service.removeByModerator(moderatorId, productId, null);

        assertEquals("Incumplimiento de politicas", result.moderationReason());
    }

    @Test
    void removeByModerator_shouldThrowWhenProductNotFound() {
        UUID moderatorId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.removeByModerator(moderatorId, productId, new ModerationRequest("Razon")));
    }

    @Test
    void appeal_shouldThrowWhenProductHasNoModerationRemoval() {
        UUID sellerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Product product = new Product(sellerId, "Producto Normal", "Desc", "cat", "http://img.png",
                BigDecimal.valueOf(20000), 5);

        when(productRepository.findByIdAndSellerId(productId, sellerId)).thenReturn(Optional.of(product));

        assertThrows(IllegalArgumentException.class,
                () -> service.appeal(sellerId, productId, new ModerationRequest("Apelo la decision")));
    }

    @Test
    void restoreAppeal_shouldActivateProductAndNotifySeller() {
        UUID productId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        Product product = new Product(sellerId, "Producto Apelado", "Desc", "cat", "http://img.png",
                BigDecimal.valueOf(15000), 3);
        product.removeByModerator(UUID.randomUUID(), "Razon temporal");
        product.requestAppeal("Solicito revision");

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        var result = service.restoreAppeal(productId, new ModerationRequest("Apelacion valida"));

        assertEquals(ProductStatus.ACTIVE, result.status());
        assertNull(result.moderationReason());
        verify(notifications).notify(eq(sellerId), eq("PRODUCT_APPEAL_APPROVED"), anyString(), anyString(), anyString());
    }

    @Test
    void rejectAppeal_shouldKeepInactiveAndNotifySeller() {
        UUID productId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        Product product = new Product(sellerId, "Producto Rechazado", "Desc", "cat", "http://img.png",
                BigDecimal.valueOf(15000), 3);
        product.removeByModerator(UUID.randomUUID(), "Razon firme");
        product.requestAppeal("Apelo");

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        var result = service.rejectAppeal(productId, new ModerationRequest("Se mantiene la decision"));

        assertEquals(ProductStatus.INACTIVE, result.status());
        assertEquals("Se mantiene la decision", result.appealResolutionNote());
        verify(notifications).notify(eq(sellerId), eq("PRODUCT_APPEAL_REJECTED"), anyString(), anyString(), anyString());
    }

    @Test
    void listMine_shouldReturnProductsForSeller() {
        UUID sellerId = UUID.randomUUID();
        Product p1 = new Product(sellerId, "Prod1", "Desc", "cat", "http://img.png", BigDecimal.valueOf(10000), 5);
        Product p2 = new Product(sellerId, "Prod2", "Desc", "cat", "http://img.png", BigDecimal.valueOf(20000), 3);

        when(productRepository.findBySellerIdOrderByCreatedAtDesc(sellerId)).thenReturn(List.of(p1, p2));

        var result = service.listMine(sellerId);

        assertEquals(2, result.size());
    }
}
