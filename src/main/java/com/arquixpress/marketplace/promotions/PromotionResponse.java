package com.arquixpress.marketplace.promotions;

import java.time.Instant;
import java.util.UUID;

public record PromotionResponse(UUID id, String name, Instant startsAt, Instant endsAt, String status) {
}
