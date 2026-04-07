package com.arquixpress.marketplace.notifications;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, UUID> {
    List<NotificationOutbox> findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
