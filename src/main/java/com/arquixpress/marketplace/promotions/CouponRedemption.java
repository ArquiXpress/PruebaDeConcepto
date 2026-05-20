package com.arquixpress.marketplace.promotions;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "coupon_redemption")
public class CouponRedemption {
    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID couponId;

    @Column(nullable = false)
    private UUID buyerId;

    @Column(nullable = false)
    private UUID orderId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(nullable = false)
    private Instant redeemedAt;

    protected CouponRedemption() {
    }

    public CouponRedemption(UUID couponId, UUID buyerId, UUID orderId, BigDecimal discountAmount) {
        this.id = UUID.randomUUID();
        this.couponId = couponId;
        this.buyerId = buyerId;
        this.orderId = orderId;
        this.discountAmount = discountAmount;
        this.redeemedAt = Instant.now();
    }
}
