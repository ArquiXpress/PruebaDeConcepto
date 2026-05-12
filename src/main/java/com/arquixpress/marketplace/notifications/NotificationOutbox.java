package com.arquixpress.marketplace.notifications;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_outbox")
public class NotificationOutbox {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String aggregateType;

    @Column(nullable = false)
    private UUID aggregateId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant lastAttemptAt;

    protected NotificationOutbox() {
    }

    public NotificationOutbox(String aggregateType, UUID aggregateId, String eventType, String payload) {
        this.id = UUID.randomUUID();
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public void sent() {
        attempts++;
        status = OutboxStatus.SENT;
        lastAttemptAt = Instant.now();
    }

    public void failed() {
        attempts++;
        status = attempts >= 5 ? OutboxStatus.FAILED : OutboxStatus.PENDING;
        lastAttemptAt = Instant.now();
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public OutboxStatus getStatus() {
    return status;
    }

    public UUID id() {
        return id;
    }

    public String eventType() {
        return eventType;
    }

    public String payload() {
        return payload;
    }
}
