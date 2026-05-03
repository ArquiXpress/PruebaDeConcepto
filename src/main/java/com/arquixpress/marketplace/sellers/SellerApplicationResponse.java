package com.arquixpress.marketplace.sellers;

import java.time.Instant;
import java.util.UUID;

public record SellerApplicationResponse(
        UUID id,
        UUID userId,
        String sellerType,
        String category,
        String status,
        Instant createdAt) {
}
