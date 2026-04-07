package com.arquixpress.marketplace.catalog.api;

import com.arquixpress.marketplace.catalog.ProductSummary;
import com.arquixpress.marketplace.catalog.application.CatalogService;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final CatalogService catalog;

    public ProductController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public Page<ProductSummary> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return catalog.search(query, category, page, size);
    }

    @GetMapping("/{id}")
    public ProductSummary detail(@PathVariable UUID id) {
        return catalog.detail(id);
    }
}
