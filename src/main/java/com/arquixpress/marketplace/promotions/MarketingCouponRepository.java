package com.arquixpress.marketplace.promotions;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketingCouponRepository extends JpaRepository<MarketingCoupon, UUID> {
    List<MarketingCoupon> findTop30ByOrderByCreatedAtDesc();
    Optional<MarketingCoupon> findByCodeIgnoreCase(String code);
}
