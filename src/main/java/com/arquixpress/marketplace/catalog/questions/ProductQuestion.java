package com.arquixpress.marketplace.catalog.questions;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "product_question")
public class ProductQuestion {
    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private UUID buyerId;

    @Column(nullable = false)
    private UUID sellerId;

    @Column(nullable = false)
    private String question;

    private String answer;

    private UUID answeredBy;

    private Instant answeredAt;

    @Column(nullable = false)
    private Instant createdAt;

    protected ProductQuestion() {
    }

    public ProductQuestion(UUID productId, UUID buyerId, UUID sellerId, String question) {
        this.id = UUID.randomUUID();
        this.productId = productId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.question = question.trim();
        this.createdAt = Instant.now();
    }

    public void answer(UUID answeredBy, String answer) {
        this.answeredBy = answeredBy;
        this.answer = answer.trim();
        this.answeredAt = Instant.now();
    }

    public UUID id() { return id; }
    public UUID productId() { return productId; }
    public UUID buyerId() { return buyerId; }
    public UUID sellerId() { return sellerId; }
    public String question() { return question; }
    public String answer() { return answer; }
    public UUID answeredBy() { return answeredBy; }
    public Instant answeredAt() { return answeredAt; }
    public Instant createdAt() { return createdAt; }
}
