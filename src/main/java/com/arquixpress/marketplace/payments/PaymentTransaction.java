package com.arquixpress.marketplace.payments;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_transaction")
public class PaymentTransaction {
    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID orderId;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    private String externalReference;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant processedAt;

    protected PaymentTransaction() {
    }

    public PaymentTransaction(UUID id, UUID orderId, String idempotencyKey, BigDecimal amount) {
        this.id = id;
        this.orderId = orderId;
        this.idempotencyKey = idempotencyKey;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public void apply(PaymentGatewayResult result) {
        this.status = result.status();
        this.externalReference = result.externalReference();
        this.processedAt = Instant.now();
    }

    public UUID id() { return id; }
    public UUID orderId() { return orderId; }
    public String idempotencyKey() { return idempotencyKey; }
    public PaymentStatus status() { return status; }
}
