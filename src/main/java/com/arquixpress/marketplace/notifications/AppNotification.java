package com.arquixpress.marketplace.notifications;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_notification")
public class AppNotification {
    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String body;

    private String actionUrl;

    private Instant readAt;

    @Column(nullable = false)
    private Instant createdAt;

    protected AppNotification() {
    }

    public AppNotification(UUID userId, String type, String title, String body, String actionUrl) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.actionUrl = actionUrl;
        this.createdAt = Instant.now();
    }

    public void markRead() {
        if (readAt == null) {
            readAt = Instant.now();
        }
    }

    public UUID id() { return id; }
    public UUID userId() { return userId; }
    public String type() { return type; }
    public String title() { return title; }
    public String body() { return body; }
    public String actionUrl() { return actionUrl; }
    public Instant readAt() { return readAt; }
    public Instant createdAt() { return createdAt; }
}
