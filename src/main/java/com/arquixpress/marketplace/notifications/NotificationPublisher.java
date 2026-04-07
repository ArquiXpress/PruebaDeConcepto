package com.arquixpress.marketplace.notifications;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class NotificationPublisher {
    private final NotificationOutboxRepository outbox;
    private final boolean enabled;

    NotificationPublisher(NotificationOutboxRepository outbox, @Value("${app.notifications.enabled}") boolean enabled) {
        this.outbox = outbox;
        this.enabled = enabled;
    }

    @Transactional
    @Scheduled(fixedDelayString = "${app.notifications.retry-delay}")
    void publishPending() {
        for (NotificationOutbox event : outbox.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING)) {
            if (enabled) {
                event.sent();
            } else {
                event.failed();
            }
        }
    }
}
