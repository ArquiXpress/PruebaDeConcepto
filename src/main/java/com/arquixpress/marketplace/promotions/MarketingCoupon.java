package com.arquixpress.marketplace.promotions;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "marketing_coupon")
public class MarketingCoupon {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private int discountPercent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponTargetType targetType;

    private String targetValue;

    @Column(nullable = false)
    private UUID createdBy;

    @Column(nullable = false)
    private Instant createdAt;

    protected MarketingCoupon() {
    }

    public MarketingCoupon(String code, String title, String description, int discountPercent,
            CouponTargetType targetType, String targetValue, UUID createdBy) {
        this.id = UUID.randomUUID();
        this.code = code.trim().toUpperCase();
        this.title = title.trim();
        this.description = description.trim();
        this.discountPercent = discountPercent;
        this.targetType = targetType;
        this.targetValue = targetValue == null || targetValue.isBlank() ? null : targetValue.trim().toLowerCase();
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
    }

    public UUID id() { return id; }
    public String code() { return code; }
    public String title() { return title; }
    public String description() { return description; }
    public int discountPercent() { return discountPercent; }
    public CouponTargetType targetType() { return targetType; }
    public String targetValue() { return targetValue; }
    public Instant createdAt() { return createdAt; }
}
