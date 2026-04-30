package com.arquixpress.marketplace.catalog.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.arquixpress.marketplace.catalog.Product;
import com.arquixpress.marketplace.catalog.ProductRepository;
import com.arquixpress.marketplace.catalog.api.SellerProductRequest;
import com.arquixpress.marketplace.catalog.api.SellerProductResponse;

@Service
public class SellerProductService {

    private final ProductRepository products;

    public SellerProductService(ProductRepository products) {
        this.products = products;
    }

    @Transactional(readOnly = true)
    public List<SellerProductResponse> listMine(UUID sellerId) {
        return products.findBySellerIdOrderByCreatedAtDesc(sellerId)
                .stream()
                .map(SellerProductResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SellerProductResponse detailMine(UUID sellerId, UUID productId) {
        return SellerProductResponse.from(findMine(sellerId, productId));
    }

    @Transactional
    public SellerProductResponse create(UUID sellerId, SellerProductRequest request) {
        Product product = new Product(
                sellerId,
                request.title(),
                request.description(),
                request.category(),
                request.imageUrl(),
                request.price(),
                request.stockAvailable()
        );

        if (request.normalizedStatus().name().equals("INACTIVE")) {
            product.deactivate();
        }

        return SellerProductResponse.from(products.save(product));
    }

    @Transactional
    public SellerProductResponse update(UUID sellerId, UUID productId, SellerProductRequest request) {
        Product product = findMine(sellerId, productId);
        product.updateDetails(
                request.title(),
                request.description(),
                request.category(),
                request.imageUrl(),
                request.price(),
                request.stockAvailable(),
                request.normalizedStatus()
        );
        return SellerProductResponse.from(product);
    }

    @Transactional
    public SellerProductResponse updateStock(UUID sellerId, UUID productId, int stockAvailable) {
        Product product = findMine(sellerId, productId);
        product.updateStock(stockAvailable);
        return SellerProductResponse.from(product);
    }

    @Transactional
    public SellerProductResponse activate(UUID sellerId, UUID productId) {
        Product product = findMine(sellerId, productId);
        product.activate();
        return SellerProductResponse.from(product);
    }

    @Transactional
    public SellerProductResponse deactivate(UUID sellerId, UUID productId) {
        Product product = findMine(sellerId, productId);
        product.deactivate();
        return SellerProductResponse.from(product);
    }

    private Product findMine(UUID sellerId, UUID productId) {
        return products.findByIdAndSellerId(productId, sellerId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado para este vendedor"));
    }
}