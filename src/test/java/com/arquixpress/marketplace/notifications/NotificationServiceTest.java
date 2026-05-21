package com.arquixpress.marketplace.notifications;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/*
 * RF-40: Notificaciones
 */
class NotificationServiceTest {

    private AppNotificationRepository notificationRepository;
    private NotificationService service;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(AppNotificationRepository.class);
        service = new NotificationService(notificationRepository);
    }

    @Test
    void notify_shouldPersistNotificationForUser() {
        UUID userId = UUID.randomUUID();
        when(notificationRepository.save(any(AppNotification.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.notify(userId, "ORDER_PAID", "Compra confirmada",
                "Tu compra fue aprobada.", "/mis-compras");

        verify(notificationRepository).save(any(AppNotification.class));
    }

    @Test
    void list_shouldReturnNotificationsForUser() {
        UUID userId = UUID.randomUUID();
        AppNotification n1 = new AppNotification(userId, "ORDER_PAID", "Compra confirmada",
                "Descripcion", "/mis-compras");
        AppNotification n2 = new AppNotification(userId, "PAYMENT_REJECTED", "Pago rechazado",
                "Descripcion", "/mis-compras");

        when(notificationRepository.findTop50ByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(n1, n2));

        List<AppNotificationResponse> result = service.list(userId);

        assertEquals(2, result.size());
    }

    @Test
    void unreadCount_shouldReturnNumberOfUnreadNotifications() {
        UUID userId = UUID.randomUUID();
        when(notificationRepository.countByUserIdAndReadAtIsNull(userId)).thenReturn(3L);

        long count = service.unreadCount(userId);

        assertEquals(3L, count);
    }

    @Test
    void markRead_shouldSetReadAtOnNotification() {
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        AppNotification notification = new AppNotification(userId, "SELLER_APPROVED",
                "Solicitud aprobada", "Descripcion", "/vendedor");

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        service.markRead(userId, notificationId);

        assertNotNull(notification.readAt());
    }

    @Test
    void markRead_shouldThrowWhenNotificationDoesNotBelongToUser() {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        AppNotification notification = new AppNotification(otherUserId, "ORDER_PAID",
                "Compra", "Desc", "/mis-compras");

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        assertThrows(IllegalArgumentException.class,
                () -> service.markRead(userId, notificationId));
    }

    @Test
    void markAllRead_shouldMarkAllUnreadNotificationsForUser() {
        UUID userId = UUID.randomUUID();
        AppNotification n1 = new AppNotification(userId, "ORDER_PAID", "Compra", "Desc", "/mis-compras");
        AppNotification n2 = new AppNotification(userId, "SELLER_SALE", "Venta", "Desc", "/vendedor");

        when(notificationRepository.findTop50ByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(n1, n2));

        service.markAllRead(userId);

        assertNotNull(n1.readAt());
        assertNotNull(n2.readAt());
    }

    @Test
    void markRead_shouldNotUpdateReadAtIfAlreadyRead() {
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        AppNotification notification = new AppNotification(userId, "ORDER_PAID", "Compra", "Desc", "/");
        notification.markRead();
        java.time.Instant firstReadAt = notification.readAt();

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        service.markRead(userId, notificationId);

        assertEquals(firstReadAt, notification.readAt());
    }

    @Test
    void notify_shouldStoreCorrectTypeAndTitle() {
        UUID userId = UUID.randomUUID();
        AppNotification[] saved = new AppNotification[1];
        when(notificationRepository.save(any(AppNotification.class))).thenAnswer(inv -> {
            saved[0] = inv.getArgument(0);
            return saved[0];
        });

        service.notify(userId, "SELLER_APPROVED", "Solicitud aprobada",
                "Tu cuenta ya puede vender.", "/vendedor");

        assertNotNull(saved[0]);
        assertEquals("SELLER_APPROVED", saved[0].type());
        assertEquals("Solicitud aprobada", saved[0].title());
        assertEquals(userId, saved[0].userId());
    }
}
