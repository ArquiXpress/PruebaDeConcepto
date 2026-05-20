package com.arquixpress.marketplace.promotions;

import com.arquixpress.marketplace.identity.CurrentUser;
import com.arquixpress.marketplace.identity.Role;
import com.arquixpress.marketplace.identity.RoleGuard;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/promotions")
class PromotionController {
    private final RoleGuard roles;
    private final PromotionService service;

    PromotionController(RoleGuard roles, PromotionService service) {
        this.roles = roles;
        this.service = service;
    }

    @PostMapping
    PromotionResponse create(@Valid @RequestBody PromotionRequest request, HttpServletRequest http) {
        roles.requireAny(CurrentUser.from(http), Role.ADMIN);
        if (request.endsAt().isBefore(request.startsAt())) {
            throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la fecha de inicio");
        }
        return new PromotionResponse(UUID.randomUUID(), request.name(), request.startsAt(), request.endsAt(), "ACTIVE_MOCK");
    }

    @GetMapping("/coupons")
    List<CouponResponse> coupons(HttpServletRequest http) {
        roles.requireAny(CurrentUser.from(http), Role.ADMIN, Role.SUPERADMIN);
        return service.listCoupons();
    }

    @PostMapping("/coupons")
    CouponResponse createCoupon(@Valid @RequestBody CouponCreateRequest request, HttpServletRequest http) {
        CurrentUser user = CurrentUser.from(http);
        roles.requireAny(user, Role.ADMIN, Role.SUPERADMIN);
        return service.createCoupon(request, user);
    }

    @GetMapping("/offers")
    List<OfferRequestResponse> adminOffers(HttpServletRequest http) {
        roles.requireAny(CurrentUser.from(http), Role.ADMIN, Role.SUPERADMIN);
        return service.listOffersForAdmin();
    }

    @PostMapping("/offers")
    OfferRequestResponse createOffer(@Valid @RequestBody OfferCreateRequest request, HttpServletRequest http) {
        CurrentUser user = CurrentUser.from(http);
        roles.requireAny(user, Role.ADMIN, Role.SUPERADMIN);
        return service.createOffer(request, user);
    }

    @GetMapping("/seller/offers")
    List<OfferRequestResponse> sellerOffers(HttpServletRequest http) {
        CurrentUser user = CurrentUser.from(http);
        roles.requireAny(user, Role.SELLER);
        return service.listOffersForSeller(user.id());
    }

    @PatchMapping("/seller/offers/{offerId}/accept")
    OfferRequestResponse acceptOffer(@PathVariable UUID offerId, HttpServletRequest http) {
        CurrentUser user = CurrentUser.from(http);
        roles.requireAny(user, Role.SELLER);
        return service.decideOffer(offerId, user.id(), true);
    }

    @PatchMapping("/seller/offers/{offerId}/reject")
    OfferRequestResponse rejectOffer(@PathVariable UUID offerId, HttpServletRequest http) {
        CurrentUser user = CurrentUser.from(http);
        roles.requireAny(user, Role.SELLER);
        return service.decideOffer(offerId, user.id(), false);
    }
}
