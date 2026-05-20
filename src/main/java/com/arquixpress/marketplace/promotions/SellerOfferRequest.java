package com.arquixpress.marketplace.promotions;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "seller_offer_request")
public class SellerOfferRequest {
    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID sellerId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    @Column(nullable = false)
    private int discountPercent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OfferRequestStatus status;

    @Column(nullable = false)
    private UUID createdBy;

    @Column(nullable = false)
    private Instant startsAt;

    @Column(nullable = false)
    private Instant endsAt;

    private Instant decidedAt;

    @Column(nullable = false)
    private Instant createdAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "seller_offer_product", joinColumns = @JoinColumn(name = "offer_request_id"))
    @Column(name = "product_id")
    private Set<UUID> productIds = new LinkedHashSet<>();

    protected SellerOfferRequest() {
    }

    public SellerOfferRequest(UUID sellerId, String title, String message, int discountPercent,
            Set<UUID> productIds, UUID createdBy, Instant startsAt, Instant endsAt) {
        this.id = UUID.randomUUID();
        this.sellerId = sellerId;
        this.title = title.trim();
        this.message = message.trim();
        this.discountPercent = discountPercent;
        this.productIds = new LinkedHashSet<>(productIds);
        this.status = OfferRequestStatus.PENDING;
        this.createdBy = createdBy;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.createdAt = Instant.now();
    }

    public void accept() {
        this.status = OfferRequestStatus.ACCEPTED;
        this.decidedAt = Instant.now();
    }

    public void reject() {
        this.status = OfferRequestStatus.REJECTED;
        this.decidedAt = Instant.now();
    }

    public UUID id() { return id; }
    public UUID sellerId() { return sellerId; }
    public String title() { return title; }
    public String message() { return message; }
    public int discountPercent() { return discountPercent; }
    public OfferRequestStatus status() { return status; }
    public Instant startsAt() { return startsAt; }
    public Instant endsAt() { return endsAt; }
    public Instant decidedAt() { return decidedAt; }
    public Instant createdAt() { return createdAt; }
    public Set<UUID> productIds() { return Set.copyOf(productIds); }
}
