package com.arquixpress.marketplace.notifications;

import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RF40RelevantNotificationsTest {

    @Test
    void sendsNotificationsForPurchaseOrderPaymentAndCampaignEvents() {
        NotificationService notifications = mock(NotificationService.class);
        NotificationEventService service = new NotificationEventService(notifications);
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID campaignId = UUID.randomUUID();

        service.purchaseConfirmed(userId, orderId);
        service.orderStatusChanged(userId, orderId, "EN_RUTA");
        service.paymentResult(userId, orderId, NotificationEventService.PaymentEventResult.REJECTED);
        service.campaignParticipation(userId, campaignId, "ACEPTADA");

        verify(notifications).notify(eq(userId), eq("ORDER_PAID"), contains("Compra"), contains("aprobada"), contains(orderId.toString()));
        verify(notifications).notify(eq(userId), eq("ORDER_STATUS"), contains("Estado"), contains("EN_RUTA"), contains(orderId.toString()));
        verify(notifications).notify(eq(userId), eq("PAYMENT_REJECTED"), contains("rechazado"), contains("pasarela"), contains(orderId.toString()));
        verify(notifications).notify(eq(userId), eq("CAMPAIGN_PARTICIPATION"), contains("Participacion"), contains("ACEPTADA"),
                contains(campaignId.toString()));
    }
}
