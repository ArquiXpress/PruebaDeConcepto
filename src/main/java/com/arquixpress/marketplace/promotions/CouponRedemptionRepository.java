package com.arquixpress.marketplace.promotions;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, UUID> {
    boolean existsByCouponIdAndBuyerId(UUID couponId, UUID buyerId);
}
