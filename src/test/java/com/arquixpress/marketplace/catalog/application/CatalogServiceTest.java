package com.arquixpress.marketplace.catalog.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.arquixpress.marketplace.catalog.Product;
import com.arquixpress.marketplace.catalog.ProductRepository;
import com.arquixpress.marketplace.catalog.ProductStatus;
import com.arquixpress.marketplace.catalog.ProductSummary;
import com.fasterxml.jackson.databind.ObjectMapper;

/*
RF-06 Listar productos en vistas como landing, categorías y productos destacados.
RF-07 Permitir la búsqueda de productos con filtros y ordenamiento (precio, categoría, proveedor, disponibilidad y ofertas).
RF-08 Mostrar el detalle del producto, incluyendo imágenes, precio, descripción, proveedor, stock y precio anterior en caso de oferta.
*/

class CatalogServiceTest {

    private ProductRepository productRepository;
    private CatalogService catalogService;
    private NamedParameterJdbcTemplate jdbc;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        jdbc = mock(NamedParameterJdbcTemplate.class);
        mapper = new ObjectMapper();

        @SuppressWarnings("unchecked")
        ObjectProvider<NamedParameterJdbcTemplate> catalogReadReplicaProvider =
                mock(ObjectProvider.class);

        when(catalogReadReplicaProvider.getIfAvailable()).thenReturn(null);

        catalogService = new CatalogService(
                productRepository,
                jdbc,
                mapper,
                catalogReadReplicaProvider,
                false
        );
    }

    @Test
    void search_shouldReturnProductsWhenQueryMatches() {
        Product product = createProduct(
                "Cemento gris",
                "Cemento para construccion",
                "Materiales",
                new BigDecimal("32000"),
                15
        );

        when(productRepository.searchByQuery(eq("%cemento%"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product)));

        Page<ProductSummary> result = catalogService.search("cemento", null, 0, 20);

        assertEquals(1, result.getTotalElements());
        assertEquals("Cemento gris", result.getContent().get(0).title());
        assertEquals("materiales", result.getContent().get(0).category());

        verify(productRepository).searchByQuery(eq("%cemento%"), any(Pageable.class));
        verify(productRepository, never()).searchAll(any(Pageable.class));
    }

    @Test
    void search_shouldNormalizeQueryAndCategoryBeforeSearching() {
        Product product = createProduct(
                "Taladro",
                "Taladro industrial",
                "Herramientas",
                new BigDecimal("250000"),
                5
        );

        when(productRepository.searchByQueryAndCategory(
                eq("%taladro%"),
                eq("herramientas"),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(product)));

        Page<ProductSummary> result = catalogService.search(
                "  TALADRO  ",
                "  HERRAMIENTAS  ",
                0,
                20
        );

        assertEquals(1, result.getTotalElements());
        assertEquals("Taladro", result.getContent().get(0).title());
        assertEquals("herramientas", result.getContent().get(0).category());

        verify(productRepository).searchByQueryAndCategory(
                eq("%taladro%"),
                eq("herramientas"),
                any(Pageable.class)
        );
    }

    @Test
    void search_shouldReturnAllActiveProductsWhenNoFiltersAreProvided() {
        Product product = createProduct(
                "Arena",
                "Arena fina para construccion",
                "Materiales",
                new BigDecimal("18000"),
                30
        );

        when(productRepository.searchAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product)));

        Page<ProductSummary> result = catalogService.search(null, null, 0, 20);

        assertEquals(1, result.getTotalElements());
        assertEquals("Arena", result.getContent().get(0).title());
        assertEquals("materiales", result.getContent().get(0).category());

        verify(productRepository).searchAll(any(Pageable.class));
    }

    @Test
    void search_shouldReturnProductsByCategoryWhenOnlyCategoryIsProvided() {
        Product product = createProduct(
                "Martillo",
                "Martillo de acero",
                "Herramientas",
                new BigDecimal("45000"),
                12
        );

        when(productRepository.searchByCategory(eq("herramientas"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product)));

        Page<ProductSummary> result = catalogService.search(null, " HERRAMIENTAS ", 0, 20);

        assertEquals(1, result.getTotalElements());
        assertEquals("Martillo", result.getContent().get(0).title());
        assertEquals("herramientas", result.getContent().get(0).category());

        verify(productRepository).searchByCategory(eq("herramientas"), any(Pageable.class));
        verify(productRepository, never()).searchAll(any(Pageable.class));
    }

    @Test
    void search_shouldFindProductsWithAssistedTypoFallback() {
        Product product = createProduct(
                "Iphone XS Max",
                "Celular en buen estado",
                "Tecnologia",
                new BigDecimal("700000"),
                7
        );

        when(productRepository.searchByQuery(eq("%ipone%"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(productRepository.searchAll(argThat(pageable -> pageable.getPageSize() == 500)))
                .thenReturn(new PageImpl<>(List.of(product)));

        Page<ProductSummary> result = catalogService.search("Ipone", null, 0, 20);

        assertEquals(1, result.getTotalElements());
        assertEquals("Iphone XS Max", result.getContent().get(0).title());

        verify(productRepository).searchByQuery(eq("%ipone%"), any(Pageable.class));
        verify(productRepository).searchAll(argThat(pageable -> pageable.getPageSize() == 500));
    }

    @Test
    void search_shouldLimitPageSizeToMaximum200() {
        when(productRepository.searchAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        catalogService.search(null, null, 0, 500);

        verify(productRepository).searchAll(
                argThat(pageable -> pageable.getPageSize() == 200)
        );
    }

    @Test
    void detail_shouldReturnProductWhenProductExistsAndIsActive() {
        Product product = createProduct(
                "Ladrillo",
                "Ladrillo comun",
                "Materiales",
                new BigDecimal("1200"),
                200
        );

        UUID productId = product.id();

        when(productRepository.findByIdAndStatus(productId, ProductStatus.ACTIVE))
                .thenReturn(Optional.of(product));

        ProductSummary result = catalogService.detail(productId);

        assertEquals(productId, result.id());
        assertEquals("Ladrillo", result.title());
        assertEquals("materiales", result.category());
        assertEquals(200, result.stockAvailable());

        verify(productRepository).findByIdAndStatus(productId, ProductStatus.ACTIVE);
    }

    @Test
    void detail_shouldThrowExceptionWhenProductDoesNotExist() {
        UUID productId = UUID.randomUUID();

        when(productRepository.findByIdAndStatus(productId, ProductStatus.ACTIVE))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> catalogService.detail(productId)
        );

        assertEquals("Producto no encontrado", exception.getMessage());

        verify(productRepository).findByIdAndStatus(productId, ProductStatus.ACTIVE);
    }

    private Product createProduct(
            String title,
            String description,
            String category,
            BigDecimal price,
            int stock
    ) {
        return new Product(
                UUID.randomUUID(),
                title,
                description,
                category,
                "https://example.com/image.png",
                price,
                stock
        );
    }
}
