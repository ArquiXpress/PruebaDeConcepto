package com.arquixpress.marketplace.notifications;

import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class NotificationEventService {
    private final NotificationService notifications;

    public NotificationEventService(NotificationService notifications) {
        this.notifications = notifications;
    }

    public void purchaseConfirmed(UUID userId, UUID orderId) {
        notifications.notify(userId, "ORDER_PAID", "Compra confirmada",
                "Tu compra fue aprobada y ya entro al proceso logistico.", "/mis-compras/" + orderId);
    }

    public void orderStatusChanged(UUID userId, UUID orderId, String status) {
        notifications.notify(userId, "ORDER_STATUS", "Estado del pedido actualizado",
                "Tu pedido ahora esta en estado " + status + ".", "/mis-compras/" + orderId);
    }

    public void paymentResult(UUID userId, UUID orderId, PaymentEventResult result) {
        String type = result == PaymentEventResult.APPROVED ? "PAYMENT_APPROVED" : "PAYMENT_REJECTED";
        String title = result == PaymentEventResult.APPROVED ? "Pago aprobado" : "Pago rechazado";
        notifications.notify(userId, type, title,
                "La pasarela reporto el resultado del pago de tu pedido.", "/mis-compras/" + orderId);
    }

    public void campaignParticipation(UUID sellerId, UUID campaignId, String status) {
        notifications.notify(sellerId, "CAMPAIGN_PARTICIPATION", "Participacion en campania",
                "Tu participacion en la campania quedo en estado " + status + ".", "/vendedor/campanias/" + campaignId);
    }

    public enum PaymentEventResult {
        APPROVED,
        REJECTED
    }
}
