package com.arquixpress.marketplace.promotions;

import java.time.Instant;
import java.util.UUID;

public record CouponResponse(
        UUID id,
        String code,
        String title,
        String description,
        int discountPercent,
        CouponTargetType targetType,
        String targetValue,
        Instant createdAt) {
    static CouponResponse from(MarketingCoupon coupon) {
        return new CouponResponse(coupon.id(), coupon.code(), coupon.title(), coupon.description(),
                coupon.discountPercent(), coupon.targetType(), coupon.targetValue(), coupon.createdAt());
    }
}
