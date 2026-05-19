package com.arquixpress.marketplace.catalog.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.arquixpress.marketplace.catalog.application.SellerProductService;
import com.arquixpress.marketplace.identity.CurrentUser;
import com.arquixpress.marketplace.identity.Role;
import com.arquixpress.marketplace.identity.RoleGuard;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/seller/products")
@Validated
public class SellerProductController {

    private final SellerProductService sellerProducts;
    private final RoleGuard roles;

    public SellerProductController(SellerProductService sellerProducts, RoleGuard roles) {
        this.sellerProducts = sellerProducts;
        this.roles = roles;
    }

    @GetMapping
    public List<SellerProductResponse> listMine(HttpServletRequest http) {
        CurrentUser user = seller(http);
        return sellerProducts.listMine(user.id());
    }

    @GetMapping("/operations-catalog")
    public List<SellerProductResponse> listForOperations(HttpServletRequest http) {
        CurrentUser user = CurrentUser.from(http);
        roles.requireAny(user, Role.ADMIN, Role.SUPERADMIN);
        return sellerProducts.listForOperations();
    }

    @GetMapping("/appeals")
    public List<SellerProductResponse> listPendingAppeals(HttpServletRequest http) {
        CurrentUser user = CurrentUser.from(http);
        roles.requireAny(user, Role.ADMIN, Role.SUPERADMIN);
        return sellerProducts.listPendingAppeals();
    }

    @GetMapping("/{id}")
    public SellerProductResponse detailMine(@PathVariable UUID id, HttpServletRequest http) {
        CurrentUser user = seller(http);
        return sellerProducts.detail(user, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SellerProductResponse create(
            @Valid @RequestBody SellerProductRequest request,
            HttpServletRequest http
    ) {
        CurrentUser user = seller(http);
        return sellerProducts.create(user.id(), request);
    }

    @PutMapping("/{id}")
    public SellerProductResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody SellerProductRequest request,
            HttpServletRequest http
    ) {
        CurrentUser user = seller(http);
        return sellerProducts.update(user.id(), id, request);
    }

    @PatchMapping("/{id}/stock")
    public SellerProductResponse updateStock(
            @PathVariable UUID id,
            @Valid @RequestBody StockUpdateRequest request,
            HttpServletRequest http
    ) {
        CurrentUser user = seller(http);
        return sellerProducts.updateStock(user.id(), id, request.stockAvailable());
    }

    @PatchMapping("/{id}/activate")
    public SellerProductResponse activate(@PathVariable UUID id, HttpServletRequest http) {
        CurrentUser user = seller(http);
        return sellerProducts.activate(user.id(), id);
    }

    @PatchMapping("/{id}/deactivate")
    public SellerProductResponse deactivate(@PathVariable UUID id, HttpServletRequest http) {
        CurrentUser user = seller(http);
        return sellerProducts.deactivate(user.id(), id);
    }

    @PatchMapping("/{id}/moderation-removal")
    public SellerProductResponse removeByModerator(
            @PathVariable UUID id,
            @RequestBody(required = false) ModerationRequest request,
            HttpServletRequest http
    ) {
        CurrentUser user = CurrentUser.from(http);
        roles.requireAny(user, Role.ADMIN, Role.SUPERADMIN);
        return sellerProducts.removeByModerator(user.id(), id, request);
    }

    @PatchMapping("/{id}/appeal")
    public SellerProductResponse appeal(
            @PathVariable UUID id,
            @RequestBody(required = false) ModerationRequest request,
            HttpServletRequest http
    ) {
        CurrentUser user = seller(http);
        return sellerProducts.appeal(user.id(), id, request);
    }

    @PatchMapping("/{id}/appeal/restore")
    public SellerProductResponse restoreAppeal(
            @PathVariable UUID id,
            @RequestBody(required = false) ModerationRequest request,
            HttpServletRequest http
    ) {
        CurrentUser user = CurrentUser.from(http);
        roles.requireAny(user, Role.ADMIN, Role.SUPERADMIN);
        return sellerProducts.restoreAppeal(id, request);
    }

    @PatchMapping("/{id}/appeal/reject")
    public SellerProductResponse rejectAppeal(
            @PathVariable UUID id,
            @RequestBody(required = false) ModerationRequest request,
            HttpServletRequest http
    ) {
        CurrentUser user = CurrentUser.from(http);
        roles.requireAny(user, Role.ADMIN, Role.SUPERADMIN);
        return sellerProducts.rejectAppeal(id, request);
    }

    private CurrentUser seller(HttpServletRequest http) {
        CurrentUser user = CurrentUser.from(http);
        roles.requireAny(user, Role.SELLER, Role.ADMIN, Role.SUPERADMIN);
        return user;
    }
}
