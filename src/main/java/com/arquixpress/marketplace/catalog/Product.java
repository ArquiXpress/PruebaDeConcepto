package com.arquixpress.marketplace.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "product")
public class Product {
    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID sellerId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String imageUrl;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int stockAvailable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(nullable = false)
    private Instant createdAt;

    protected Product() {
    }

    public UUID id() { return id; }
    public UUID sellerId() { return sellerId; }
    public String title() { return title; }
    public String description() { return description; }
    public String category() { return category; }
    public String imageUrl() { return imageUrl; }
    public BigDecimal price() { return price; }
    public int stockAvailable() { return stockAvailable; }
    public ProductStatus status() { return status; }
    public long version() { return version; }
    public Instant createdAt() { return createdAt; }
}
