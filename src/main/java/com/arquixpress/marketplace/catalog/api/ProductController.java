package com.arquixpress.marketplace.catalog.api;

import com.arquixpress.marketplace.catalog.ProductCreateRequest;
import com.arquixpress.marketplace.catalog.ProductSummary;
import com.arquixpress.marketplace.catalog.application.CatalogService;
import com.arquixpress.marketplace.identity.CurrentUser;
import com.arquixpress.marketplace.identity.Role;
import com.arquixpress.marketplace.identity.RoleGuard;
import com.arquixpress.marketplace.promotions.PromotionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@Validated
public class ProductController {
    private final CatalogService catalog;
    private final com.arquixpress.marketplace.catalog.ProductRepository products;
    private final RoleGuard roles;
    private final PromotionService promotions;

    public ProductController(CatalogService catalog, com.arquixpress.marketplace.catalog.ProductRepository products,
            RoleGuard roles, PromotionService promotions) {
        this.catalog = catalog;
        this.products = products;
        this.roles = roles;
        this.promotions = promotions;
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

    @GetMapping("/offers")
    public List<ProductSummary> offers() {
        return promotions.activeOfferProducts();
    }

    @PostMapping
    public ProductSummary create(@RequestBody ProductCreateRequest request, HttpServletRequest http) {
        CurrentUser user = CurrentUser.from(http);
        roles.requireAny(user, Role.SELLER, Role.ADMIN, Role.SUPERADMIN);
        return catalog.create(user, request);
    }

    @PatchMapping("/{id}/stock")
    public ProductSummary updateStock(@PathVariable UUID id, @RequestBody StockChangeRequest request, HttpServletRequest http) {
        CurrentUser user = CurrentUser.from(http);
        roles.requireAny(user, Role.SELLER, Role.ADMIN);
        int updated = products.setStock(id, Math.max(0, request.stock()));
        if (updated != 1) {
            throw new IllegalArgumentException("Producto no encontrado");
        }
        return catalog.detail(id);
    }

    public record StockChangeRequest(@Min(0) int stock) {}
}
