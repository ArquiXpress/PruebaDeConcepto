package com.arquixpress.marketplace.notifications;

import com.arquixpress.marketplace.identity.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class AppNotificationController {
    private final NotificationService notificationService;

    public AppNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<AppNotificationResponse> list(HttpServletRequest http) {
        return notificationService.list(CurrentUser.from(http).id());
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(HttpServletRequest http) {
        return Map.of("count", notificationService.unreadCount(CurrentUser.from(http).id()));
    }

    @PatchMapping("/{id}/read")
    public void markRead(@PathVariable UUID id, HttpServletRequest http) {
        notificationService.markRead(CurrentUser.from(http).id(), id);
    }

    @PatchMapping("/read-all")
    public void markAllRead(HttpServletRequest http) {
        notificationService.markAllRead(CurrentUser.from(http).id());
    }
}
