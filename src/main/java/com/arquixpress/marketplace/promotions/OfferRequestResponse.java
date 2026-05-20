package com.arquixpress.marketplace.promotions;

import com.arquixpress.marketplace.catalog.Product;
import com.arquixpress.marketplace.identity.AppUser;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record OfferRequestResponse(
        UUID id,
        UUID sellerId,
        String sellerName,
        String sellerEmail,
        String title,
        String message,
        int discountPercent,
        OfferRequestStatus status,
        Instant startsAt,
        Instant endsAt,
        Instant createdAt,
        Instant decidedAt,
        List<OfferProductResponse> products) {
    static OfferRequestResponse from(SellerOfferRequest offer, AppUser seller, Map<UUID, Product> products) {
        return new OfferRequestResponse(
                offer.id(),
                offer.sellerId(),
                seller == null ? "" : seller.displayName(),
                seller == null ? "" : seller.email(),
                offer.title(),
                offer.message(),
                offer.discountPercent(),
                offer.status(),
                offer.startsAt(),
                offer.endsAt(),
                offer.createdAt(),
                offer.decidedAt(),
                offer.productIds().stream()
                        .map(products::get)
                        .filter(product -> product != null)
                        .map(OfferProductResponse::from)
                        .toList());
    }
}
