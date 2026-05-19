package com.arquixpress.marketplace.catalog;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "product")
public class Product {
    private static final ObjectMapper MAPPER = new ObjectMapper();

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

    @Column(nullable = false, columnDefinition = "text")
    private String imageUrl;

    @Column(columnDefinition = "text")
    private String imageUrls;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int stockAvailable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;

    private String moderationReason;

    private UUID moderationBy;

    private Instant moderationAt;

    private String appealNote;

    private Instant appealRequestedAt;

    private String appealResolutionNote;

    private Instant appealResolvedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(nullable = false)
    private Instant createdAt;

    protected Product() {
    }

    public Product(
            UUID sellerId,
            String title,
            String description,
            String category,
            String imageUrl,
            BigDecimal price,
            int stockAvailable
    ) {
        this.id = UUID.randomUUID();
        this.sellerId = sellerId;
        this.title = title.trim();
        this.description = description.trim();
        this.category = category.trim().toLowerCase();
        this.imageUrl = imageUrl.trim();
        this.imageUrls = toJson(List.of(this.imageUrl));
        this.price = price;
        this.stockAvailable = Math.max(0, stockAvailable);
        this.status = this.stockAvailable == 0 ? ProductStatus.INACTIVE : ProductStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    public UUID id() { return id; }
    public UUID sellerId() { return sellerId; }
    public String title() { return title; }
    public String description() { return description; }
    public String category() { return category; }
    public String imageUrl() { return imageUrl; }
    public String imageUrls() { return imageUrls; }
    public BigDecimal price() { return price; }
    public int stockAvailable() { return stockAvailable; }
    public ProductStatus status() { return status; }
    public String moderationReason() { return moderationReason; }
    public UUID moderationBy() { return moderationBy; }
    public Instant moderationAt() { return moderationAt; }
    public String appealNote() { return appealNote; }
    public Instant appealRequestedAt() { return appealRequestedAt; }
    public String appealResolutionNote() { return appealResolutionNote; }
    public Instant appealResolvedAt() { return appealResolvedAt; }
    public long version() { return version; }
    public Instant createdAt() { return createdAt; }

    public void updateDetails(
            String title,
            String description,
            String category,
            String imageUrl,
            BigDecimal price,
            int stockAvailable,
            ProductStatus status
    ) {
        this.title = title.trim();
        this.description = description.trim();
        this.category = category.trim().toLowerCase();
        this.imageUrl = imageUrl.trim();
        this.imageUrls = toJson(List.of(this.imageUrl));
        this.price = price;
        this.stockAvailable = Math.max(0, stockAvailable);
        this.status = this.stockAvailable == 0 ? ProductStatus.INACTIVE : status;
    }

    public void updateDetails(
            String title,
            String description,
            String category,
            List<String> imageUrls,
            BigDecimal price,
            int stockAvailable,
            ProductStatus status
    ) {
        List<String> cleanImages = cleanImages(imageUrls);
        this.title = title.trim();
        this.description = description.trim();
        this.category = category.trim().toLowerCase();
        this.imageUrl = cleanImages.get(0);
        this.imageUrls = toJson(cleanImages);
        this.price = price;
        this.stockAvailable = Math.max(0, stockAvailable);
        this.status = this.stockAvailable == 0 ? ProductStatus.INACTIVE : status;
    }

    public void updateStock(int stockAvailable) {
        this.stockAvailable = Math.max(0, stockAvailable);
        if (this.stockAvailable == 0) {
            this.status = ProductStatus.INACTIVE;
        }
    }

    public void activate() {
        if (stockAvailable == 0) {
            this.status = ProductStatus.INACTIVE;
            return;
        }
        this.status = ProductStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = ProductStatus.INACTIVE;
    }

    public void removeByModerator(UUID moderatorId, String reason) {
        this.status = ProductStatus.INACTIVE;
        this.moderationBy = moderatorId;
        this.moderationReason = reason == null || reason.isBlank() ? "Incumplimiento de politicas" : reason.trim();
        this.moderationAt = Instant.now();
        this.appealNote = null;
        this.appealRequestedAt = null;
        this.appealResolutionNote = null;
        this.appealResolvedAt = null;
    }

    public void requestAppeal(String note) {
        this.appealNote = note == null || note.isBlank() ? "Solicito revision de la publicacion" : note.trim();
        this.appealRequestedAt = Instant.now();
        this.appealResolutionNote = null;
        this.appealResolvedAt = null;
    }

    public void restoreAfterAppeal(String note) {
        this.status = stockAvailable == 0 ? ProductStatus.INACTIVE : ProductStatus.ACTIVE;
        this.moderationReason = null;
        this.moderationBy = null;
        this.moderationAt = null;
        this.appealResolutionNote = note == null || note.isBlank() ? "Apelacion aprobada" : note.trim();
        this.appealResolvedAt = Instant.now();
    }

    public void rejectAppeal(String note) {
        this.status = ProductStatus.INACTIVE;
        this.appealResolutionNote = note == null || note.isBlank() ? "Se mantiene la eliminacion de la publicacion" : note.trim();
        this.appealResolvedAt = Instant.now();
    }

    public List<String> parsedImageUrls() {
        return parseImages(imageUrls, imageUrl);
    }

    public static List<String> parseImages(String imageUrls, String fallbackImageUrl) {
        if (imageUrls == null || imageUrls.isBlank()) {
            return List.of(fallbackImageUrl);
        }
        try {
            List<String> parsed = MAPPER.readValue(imageUrls, new TypeReference<>() {});
            List<String> clean = cleanImages(parsed);
            return clean.isEmpty() ? List.of(fallbackImageUrl) : clean;
        } catch (JsonProcessingException ex) {
            return List.of(fallbackImageUrl);
        }
    }

    private static List<String> cleanImages(List<String> images) {
        List<String> clean = images == null ? List.of() : images.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        if (clean.isEmpty()) {
            throw new IllegalArgumentException("Agrega al menos una foto del producto");
        }
        return clean;
    }

    private static String toJson(List<String> images) {
        try {
            return MAPPER.writeValueAsString(cleanImages(images));
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("No se pudieron procesar las imagenes");
        }
    }
}
