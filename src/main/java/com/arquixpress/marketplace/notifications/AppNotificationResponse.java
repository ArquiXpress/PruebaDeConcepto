package com.arquixpress.marketplace.notifications;

import java.time.Instant;
import java.util.UUID;

public record AppNotificationResponse(
        UUID id,
        String type,
        String title,
        String body,
        String actionUrl,
        boolean read,
        Instant createdAt
) {
    public static AppNotificationResponse from(AppNotification notification) {
        return new AppNotificationResponse(
                notification.id(),
                notification.type(),
                notification.title(),
                notification.body(),
                notification.actionUrl(),
                notification.readAt() != null,
                notification.createdAt());
    }
}
