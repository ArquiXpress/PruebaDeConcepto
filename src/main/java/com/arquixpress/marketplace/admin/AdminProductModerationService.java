package com.arquixpress.marketplace.admin;

import com.arquixpress.marketplace.catalog.Product;
import com.arquixpress.marketplace.catalog.ProductRepository;
import com.arquixpress.marketplace.catalog.api.SellerProductResponse;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminProductModerationService {
    private final ProductRepository products;

    public AdminProductModerationService(ProductRepository products) {
        this.products = products;
    }

    @Transactional
    public SellerProductResponse approve(UUID productId) {
        Product product = find(productId);
        product.activate();
        return SellerProductResponse.from(products.save(product));
    }

    @Transactional
    public SellerProductResponse reject(UUID productId) {
        Product product = find(productId);
        product.deactivate();
        return SellerProductResponse.from(products.save(product));
    }

    @Transactional
    public SellerProductResponse suspend(UUID productId) {
        Product product = find(productId);
        product.suspend();
        return SellerProductResponse.from(products.save(product));
    }

    private Product find(UUID productId) {
        return products.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
    }
}
