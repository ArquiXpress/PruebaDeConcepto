package com.arquixpress.marketplace.notifications;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final AppNotificationRepository notifications;

    public NotificationService(AppNotificationRepository notifications) {
        this.notifications = notifications;
    }

    public void notify(UUID userId, String type, String title, String body, String actionUrl) {
        notifications.save(new AppNotification(userId, type, title, body, actionUrl));
    }

    public List<AppNotificationResponse> list(UUID userId) {
        return notifications.findTop50ByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(AppNotificationResponse::from)
                .toList();
    }

    public long unreadCount(UUID userId) {
        return notifications.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public void markRead(UUID userId, UUID notificationId) {
        AppNotification notification = notifications.findById(notificationId)
                .filter(item -> item.userId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Notificacion no encontrada"));
        notification.markRead();
    }

    @Transactional
    public void markAllRead(UUID userId) {
        notifications.findTop50ByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(item -> item.readAt() == null)
                .forEach(AppNotification::markRead);
    }
}
